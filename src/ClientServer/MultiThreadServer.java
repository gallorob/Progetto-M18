package ClientServer;

import TradeCenter.Exceptions.TradeExceptions.AlreadyStartedTradeException;
import TradeCenter.Exceptions.UserExceptions.CheckPasswordConditionsException;
import TradeCenter.Exceptions.UserExceptions.UsernameAlreadyTakenException;
import TradeCenter.TradeCenter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class MultiThreadServer implements Runnable {
    private Socket csocket;
    private static TradeCenter tradeCenter = new TradeCenter();
    private ServerProxy proxy;
    Map<MessageType, Method> methodMap = new HashMap<>();

    MultiThreadServer(Socket csocket, TradeCenter tradeCenter) throws NoSuchMethodException, SocketException {
        this.csocket = csocket;
        csocket.setTcpNoDelay(true);
        this.tradeCenter = tradeCenter;
        proxy = new ServerProxy(tradeCenter);

        methodMap.put(MessageType.VERIFYPASSWORD, ServerProxy.class.getMethod("verifyPassword", MessageServer.class));
        methodMap.put(MessageType.LOGDIN, ServerProxy.class.getMethod("loggedIn", MessageServer.class));
        methodMap.put(MessageType.ADDCUSTOMER, ServerProxy.class.getMethod("addCustomer", MessageServer.class));
        methodMap.put(MessageType.SEARCHCUSTOMER, ServerProxy.class.getMethod("searchCustomer", MessageServer.class));
        methodMap.put(MessageType.POSSIBLETRADE, ServerProxy.class.getMethod("possibleTrade", MessageServer.class));
        methodMap.put(MessageType.CREATEOFFER, ServerProxy.class.getMethod("createTrade", MessageServer.class));
        methodMap.put(MessageType.RAISEOFFER, ServerProxy.class.getMethod("updateOffer", MessageServer.class));
        methodMap.put(MessageType.SEARCHOFFER, ServerProxy.class.getMethod("searchOffer", MessageServer.class));
        methodMap.put(MessageType.SEARCHTRADE, ServerProxy.class.getMethod("searchTrade", MessageServer.class));
        methodMap.put(MessageType.ENDTRADE, ServerProxy.class.getMethod("endTrades", MessageServer.class));
        methodMap.put(MessageType.SEARCHDESCRIPTION, ServerProxy.class.getMethod("searchDescription", MessageServer.class));
        methodMap.put(MessageType.SEARCHUSER, ServerProxy.class.getMethod("searchUsers", MessageServer.class));
        methodMap.put(MessageType.REMOVEWISH, ServerProxy.class.getMethod("removeFromWishList", MessageServer.class));
        methodMap.put(MessageType.ADDCARDPOKEMON, ServerProxy.class.getMethod("addPokemonRandom", MessageServer.class));
        methodMap.put(MessageType.ADDCARDYUGI, ServerProxy.class.getMethod("addYuGiOhRandom", MessageServer.class));
        methodMap.put(MessageType.SEARCHUSERBYID, ServerProxy.class.getMethod("searchCustomerByID", MessageServer.class));


    }
    public static void main(String args[]) throws Exception {
        ServerSocket ssock = new ServerSocket(8889);
        System.out.println("Listening");

        while (true) {
            Socket sock = ssock.accept();
            System.out.println("Connected");
            new Thread(new MultiThreadServer(sock, tradeCenter)).start();
        }
    }
    public void run() throws CheckPasswordConditionsException, UsernameAlreadyTakenException {
        try {
            ObjectInputStream in = new ObjectInputStream(csocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(csocket.getOutputStream());
            MessageServer m = (MessageServer) in.readObject();
            Object result = returnMessage(m.getMessage(),m);
            os.writeObject(result);
            os.flush();
            csocket.close();

        } catch (IOException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Object returnMessage(MessageType tag, MessageServer messageServer) throws InvocationTargetException, IllegalAccessException, CheckPasswordConditionsException {
        Object result = null;
        try {
            result = methodMap.get(tag).invoke(proxy ,messageServer);
        }
        catch (InvocationTargetException e){
            if(e.getCause() instanceof AlreadyStartedTradeException){
                result = false;
            }else {
                result = e.getCause();
            }
        }


        return result;
    }
}
