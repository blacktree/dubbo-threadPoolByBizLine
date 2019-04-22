package lz.dubbo.channelHandler;

import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.ExecutionException;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelEventRunnable;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelEventRunnable.ChannelState;
import com.alibaba.dubbo.remoting.transport.dispatcher.all.AllChannelHandler;
import com.alibaba.dubbo.rpc.Invocation;

import lz.dubbo.threadPool.SeprExecutorService;

public class SeprChannelHandler extends AllChannelHandler {


	private final Log logger=LogFactory.getLog(this.getClass());

	private SeprExecutorService executorService;
	
	public SeprChannelHandler(ChannelHandler handler, URL url,SeprExecutorService executorService) {
		
		super(handler, url);
		this.executorService=executorService;
		
	}

	@Override
	public void received(Channel channel, Object message) throws RemotingException {
		
       ExecutorService exec=executor;
       
       try {
    	   if(message instanceof Request) {
    		   
    		   Request req=(Request) message;
    		   Object msg=req.getData();
    		   
    		   if(msg instanceof Invocation) {
    			   Invocation inv=(Invocation) msg;
    			   String serviceName=inv.getAttachment("path");
    			   if(executorService !=null) {
    				   exec=executorService.getExecutorService(serviceName);
    			   }
    			   if(exec==null||exec.isShutdown()) {
    				   exec=SHARED_EXECUTOR;
    			   }
    		   }
    	   }
    	    
       }catch(Throwable e) {
    	   logger.error(e.getMessage(), e);
       }
		
       try {
    	   exec.execute(new ChannelEventRunnable(channel,handler,ChannelState.RECEIVED,message));
       }catch(Throwable e) {
    	   logger.error(e.getMessage(), e);
    	   throw new ExecutionException(message,channel,getClass()+" error when process received event",e);
       }
		
	}

	
	
	
}
