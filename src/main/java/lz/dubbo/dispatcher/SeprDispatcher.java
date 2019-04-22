package lz.dubbo.dispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.transport.dispatcher.all.AllDispatcher;

import lz.dubbo.channelHandler.SeprChannelHandler;
import lz.dubbo.threadPool.SeprExecutorService;


//服务隔离分发器
public class SeprDispatcher extends AllDispatcher {
	
	private final Log logger=LogFactory.getLog(this.getClass());

	private SeprExecutorService executorService;
	
	public ChannelHandler dispatch(ChannelHandler handler,URL url) {
		return new SeprChannelHandler(handler,url,executorService);
	}

	public SeprExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(SeprExecutorService executorService) {
		this.executorService = executorService;
	}
	
	

}
