package lz.dubbo.threadPool;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.StringUtils;

public class SeprExecutorService implements InitializingBean,DisposableBean {

	private final Log logger=LogFactory.getLog(this.getClass());

	private Map<String,ThreadPoolExecutor> execList=new ConcurrentHashMap<String,ThreadPoolExecutor>();

	// 定义 领域 服务的线程池参数，如 sale:5,10,3;member:10,20,5......
	@Value("seprCfg")
	private String seprCfg;

	public ExecutorService getExecutorService(String serviceName) {

		ExecutorService exec=null;

		String[] splitNames=StringUtils.split(serviceName, '.');

		String regionName=splitNames[0];

		if(splitNames.length <=1 )
			regionName=Constants.DEFAULT_THREAD_NAME;

		return exec=execList.get(regionName);		

	}

	@Override
	public void destroy() throws Exception {
		this.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		String[] splitRegions=StringUtils.split(seprCfg, ';');
		ExecutorService exec=null;
		if(splitRegions.length >0) {
			for(String splitRegion:splitRegions) {
				String[] threadCfg=StringUtils.split(seprCfg, ',');

				String regionName=threadCfg[0];
				int coreSize=Integer.valueOf(threadCfg[1]);
				int maxSize=Integer.valueOf(threadCfg[2]);
				long keepAliveTime=Long.valueOf(threadCfg[3]);


				exec=new ThreadPoolExecutor(coreSize,maxSize,
						keepAliveTime,TimeUnit.MINUTES,
						new SynchronousQueue<Runnable>(),new NamedThreadFactory(regionName,true),
						new ExAbortPolicyWithReport(regionName));

				execList.put(regionName, (ThreadPoolExecutor)exec);
			}				
		}
		//default dubbo pool
		exec=new ThreadPoolExecutor(Constants.DEFAULT_THREADS, Constants.DEFAULT_THREADS, 0, TimeUnit.MILLISECONDS,
				new SynchronousQueue<Runnable>(),
				new NamedThreadFactory(Constants.DEFAULT_THREAD_NAME, true), new ExAbortPolicyWithReport(Constants.DEFAULT_THREAD_NAME));

		execList.put(Constants.DEFAULT_THREAD_NAME, (ThreadPoolExecutor)exec);

	}
	

	public void close() {
		for(Entry<String,ThreadPoolExecutor> e:execList.entrySet()) {
			try {
				e.getValue().shutdown();
				logger.info("destroy thread pool of "+e.getKey());
			}catch(Throwable t) {
				logger.warn("fail to destory thread pool of "+e.getKey() +":"+t.getMessage(),t);
			}
		}
	}


	class ExAbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy{


		private final Log logger=LogFactory.getLog(this.getClass());

		private final String threadName;
		private final String ip;

		public ExAbortPolicyWithReport(String regionName) {
			threadName=regionName;
			ip=getLocalHost();
		}

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

			String msg=String.format("threadPool is EXHAUSTED!"+ " name: %s,pool size: %d (active: %d,core: %d,max: %d"
					+ ",largest: %d),task: %d(completed: %d),Executor status:(isShutdown:%/s,isTerminated:%s,isTerminating:%s ),"
					+ "in Ip %s", threadName,e.getPoolSize(),e.getActiveCount(),e.getCorePoolSize(),
					e.getMaximumPoolSize(),e.getLargestPoolSize(),e.getTaskCount(),e.getCompletedTaskCount(),
					e.isShutdown(),e.isTerminated(),e.isTerminating(),ip);

			logger.error(msg);

			throw new RejectedExecutionException(msg);

		}
	}

	public static String getLocalHost()  {

		try {
			InetAddress address = InetAddress.getLocalHost();
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "127.0.0.1";
	}


}
