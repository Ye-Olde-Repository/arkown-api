package com.roylaurie.arkown.apiservice;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.roylaurie.arkown.json.ServerDeleteRequestJsonEnvelope;
import com.roylaurie.arkown.json.ServerDeleteResponseJsonEnvelope;
import com.roylaurie.arkown.json.ServerListRequestJsonEnvelope;
import com.roylaurie.arkown.json.ServerListResponseJsonEnvelope;
import com.roylaurie.arkown.json.ServerUpdateRequestJsonEnvelope;
import com.roylaurie.arkown.json.ServerUpdateResponseJsonEnvelope;
import com.roylaurie.arkown.json.ServerListRequestJsonEnvelope.Host;
import com.roylaurie.arkown.server.Server;
import com.roylaurie.arkown.server.sql.ServerSql;
import com.roylaurie.modeljson.JsonUriAction;
import com.roylaurie.modeljson.JsonEnvelope;
import com.roylaurie.modeljson.JsonFactory.ExclusionStrategyType;
import com.roylaurie.modeljson.exception.JsonException;
import com.roylaurie.modelsql.ModelSql;
import com.roylaurie.modelsql.ModelSql.ColumnQueryList;
import com.roylaurie.modelsql.ModelSql.ModelSqlDuplicateException;

/**
 * Servlet implementation class ServerServlet
 */
@SuppressWarnings("serial")
public final class ServerServlet extends HttpServlet {
    private HttpServletRequest mServletRequest = null;
    private HttpServletResponse mServletResponse = null;
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServerServlet() {
        super();
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
	    
	    ModelSql.setConfigurationFilepath(
	            config.getServletContext().getRealPath("WEB-INF/res/xml/com/roylaurie/sql/arkown.xml")
        );
	}

    /**
     * Sets the servletRequest.
     *
     * @param HttpServletRequest servletRequest
     */
    private void initializeApi(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        mServletRequest = servletRequest;
        mServletResponse = servletResponse;
    }

    /**
     * Retrieves the servletRequest.
     *
     * @return HttpServletRequest
     */
    private HttpServletRequest getServletRequest() {
        return mServletRequest;
    }

    /**
     * Retrieves the servletResponse.
     *
     * @return HttpServletResponse
     */
    private HttpServletResponse getServletResponse() {
        return mServletResponse;
    }

    private Server findProxiedServer(String hostname, int port) throws SQLException {
        ServerSql readServerSql = new ServerSql(ModelSql.CONTEXT_READ);
        ColumnQueryList queryList = new ColumnQueryList();
        String address = null;
        
        try {
            address = InetAddress.getByName(hostname).getHostAddress();
        } catch (UnknownHostException e) {
            address = null;
        }
        
        if (address != null) {
            queryList.and(new ColumnQueryList()
                .and(ServerSql.Column.HOSTNAME, hostname)
                .or(ServerSql.Column.HOST_ADDRESS, address)
            );
        } else {
            queryList.and(ServerSql.Column.HOSTNAME, hostname);
        }
        
        queryList.and(ServerSql.Column.PORT, port)
        .and(ServerSql.Column.IS_QUERY_PROXY_ALLOWED, true);
        
        List<Server> serverList = readServerSql.find(queryList, 1);
        if (serverList.isEmpty()) {
            throw new IndexOutOfBoundsException("Server not found.");
        }
        
        return serverList.get(0);
    }	
	
    private List<Server> findProxiedServers(List<Host> hostList) {
        ServerSql readServerSql = new ServerSql(ModelSql.CONTEXT_READ);
        ArrayList<Server> serverList = new ArrayList<Server>();
        Server server = null;
        
        for (Host host : hostList) {
            if (host.getServerId() > 0) {
                try {
                    server = readServerSql.read(host.getServerId());
                } catch(IndexOutOfBoundsException e) {
                    continue;
                } catch (SQLException e) {
                    continue;
                }

                if (server.isQueryProxyAllowed()) {
                    serverList.add(server);
                }

                continue;
            }
            
            String address = null;
            try {
                address = InetAddress.getByName(host.getHostname()).getHostAddress();
            } catch (UnknownHostException e) {
                address = null;
            }
            
            ColumnQueryList queryList = new ColumnQueryList();
            
            if (address != null) {
                queryList.and(new ColumnQueryList()
                    .and(ServerSql.Column.HOSTNAME, host.getHostname())
                    .or(ServerSql.Column.HOST_ADDRESS, address)
                );
            } else {
                queryList.and(ServerSql.Column.HOSTNAME, host.getHostname());
            }
            
            queryList.and(ServerSql.Column.PORT, host.getPort())
            .and(ServerSql.Column.IS_QUERY_PROXY_ALLOWED, true);
            
            List<Server> results;
            try {
                results = readServerSql.find(queryList);
            } catch (SQLException e) {
                continue;
            }
            
            if (!results.isEmpty()) {
                serverList.add(results.get(0));
            }
        }            

        return serverList;
    }   
	
    private void writeServer(Server server)
            throws SQLException, IndexOutOfBoundsException {
        ServerSql writeServerSql = new ServerSql(ModelSql.CONTEXT_WRITE);
        writeServerSql.write(server);        
    }
    
    private Server readServer(long id) throws SQLException {
        ServerSql readServerSql = new ServerSql(ModelSql.CONTEXT_READ);
        Server server = readServerSql.read(id);
        
        return server;
    }
    
    private  void deleteServer(Server server) throws SQLException {
        ServerSql writeServerSql = new ServerSql(ModelSql.CONTEXT_WRITE);
        writeServerSql.delete(server.getDatabaseId());        
    }        
    
    private final void sendResponse(JsonEnvelope jsonEnvelope) throws IOException {
        HttpServletResponse response = getServletResponse();
        
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().print(jsonEnvelope);           
    }
    
    private final String getRequestContent() throws IOException {
        BufferedInputStream input = new BufferedInputStream(getServletRequest().getInputStream());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        int result = input.read();
        while(result != -1) {
            byte b = (byte)result;
            output.write(b);
            result = input.read();
        }       
        
        return output.toString();
    }
    
    /**
     * Hander requests.
     *
     * @param HttpServletRequest servletRequest
     * @param HttpServletResponse servletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {
        initializeApi(servletRequest, servletResponse);
        
        String requestContent = getRequestContent();
        JsonEnvelope response = null;
       
        try {
            switch (JsonUriAction.parseFromUri(servletRequest.getPathInfo())) {
            case FIND:
                response = doPostFind(requestContent);
                break;
            /*    
            case READ:
                response = doPostRead(requestContent);
                break;
            */    
            case WRITE:
                response = doPostWrite(requestContent);
                break;
                
            case DELETE:
                response = doPostDelete(requestContent);
                break;
                
            default:
                servletResponse.sendError(404);
                return;
            }
        } catch (Exception e) {
            servletResponse.sendError(500);
            return;
        }
        if (response == null) {
            servletResponse.sendError(500);
            return;
        }
        
        try {
            sendResponse(response);
        } catch (Exception e) {
            servletResponse.sendError(500);
            return;
        }
	}
    
    private JsonEnvelope doPostFind(String requestContent) throws JsonException {
        ServerListRequestJsonEnvelope request = new ServerListRequestJsonEnvelope(requestContent);
        ServerListResponseJsonEnvelope response =  new ServerListResponseJsonEnvelope(ExclusionStrategyType.PUBLIC);
    
        for (Server server : findProxiedServers(request.getHostList())) {
            response.addServer(server);
        }
  
        return response;      
    }
    
 
    /**
     * 
     *
     * @param requestContent
     * @return
     * @throws JsonException
     */
    private JsonEnvelope doPostWrite(String requestContent) throws JsonException {
        ServerUpdateRequestJsonEnvelope request = new ServerUpdateRequestJsonEnvelope(requestContent);
        ServerUpdateResponseJsonEnvelope response = new ServerUpdateResponseJsonEnvelope();
        Server server = request.getServer();
        Server originalServer = null;
        boolean isServerAuthenticated = false;
        
        if (server.getDatabaseId() != 0) { // check existance and permissions first before updating
            try {
                originalServer = readServer(server.getDatabaseId());
            } catch (SQLException e) {
                response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_UNKNOWN);
                return response;
            } catch (IndexOutOfBoundsException e) {
                response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_NOT_FOUND);
                return response;
            }
            
            //TODO: ensure rcon pull and use that instead
            if (!originalServer.getCredentials().equals(server.getCredentials())) {
                if (!server.validateCredentials()) {
                    response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_PERMISSION);
                    return response;
                }
                
                isServerAuthenticated = true;
            }            
        }
        
        if (!isServerAuthenticated && !server.validateCredentials()) {
            response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_PERMISSION);            
            return response;
        }        
        
        isServerAuthenticated = true; // for posterity
        
        try {
            server.pull();
        } catch (Exception e) {
            response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_QUERY);
            return response;
        }
        
        try {
            writeServer(server);
        }
        catch (ModelSqlDuplicateException e) {
            response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_CREATE_DUPLICATE);
            response.setServer((Server)e.getDuplicateObject());    
            return response;
        } catch (SQLException e) {
            if (e.getSQLState().equals(ModelSql.SQLSTATE_DUPLICATE)) {
                try {
                    originalServer = findProxiedServer(server.getHostname(), server.getPort());
                    response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_CREATE_DUPLICATE);
                    response.setServer(originalServer);
                } catch (Exception e2) {
                    response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_UNKNOWN);
                }
            } else {
                response.setResult(ServerUpdateResponseJsonEnvelope.Result.ERROR_UNKNOWN);
            }

            return response;
        }
        
        
        response.setServer(server);
        response.setResult(ServerUpdateResponseJsonEnvelope.Result.OK);    
        return response;
    }


    /**
     * Deletes an existing server.
     *
     * @param String requestContent
     * @return JsonEnvelope
     * @throws JsonException
     */
    private JsonEnvelope doPostDelete(String requestContent) throws JsonException {
        ServerDeleteRequestJsonEnvelope request = new ServerDeleteRequestJsonEnvelope(requestContent);
        ServerDeleteResponseJsonEnvelope response = new ServerDeleteResponseJsonEnvelope();
        Server deleteServer = null;
        
        try {
            deleteServer = readServer(request.getServer().getDatabaseId());
        } catch (SQLException e) {
            response.setResult(ServerDeleteResponseJsonEnvelope.Result.ERROR_UNKNOWN);
            return response;
        } catch (IndexOutOfBoundsException e) {
            response.setResult(ServerDeleteResponseJsonEnvelope.Result.ERROR_NOT_FOUND);
            return response;
        }        
        
        if (!deleteServer.getCredentials().equals(request.getServer().getCredentials())) {
            response.setResult(ServerDeleteResponseJsonEnvelope.Result.ERROR_PERMISSION);
            return response;
        }
        
        try {
            deleteServer(deleteServer);
        } catch (SQLException e) {
            response.setResult(ServerDeleteResponseJsonEnvelope.Result.ERROR_UNKNOWN);
            return response;
        }
        
        response.setResult(ServerDeleteResponseJsonEnvelope.Result.OK);
        
        return response;     
    }

}
