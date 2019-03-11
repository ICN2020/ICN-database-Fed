package com.ogb.fes.concurrent;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Parallel
{
	static final int iCPU = Runtime.getRuntime().availableProcessors();

	public static <T> void ForEach(Iterable <T> parameters, final LoopBodyArgs<T> loopBody)
	{
	    ExecutorService executor = Executors.newFixedThreadPool(iCPU);
	    List<Future<?>> futures  = new LinkedList<Future<?>>();
	
	    for (final T param : parameters)
	    {
	        Future<?> future = executor.submit(new Runnable()
	        {
	            public void run() { loopBody.run(param); }
	        });
	
	        futures.add(future);
	    }
	
	    for (Future<?> f : futures)
	    {
	        try   { f.get(); }
	        catch (InterruptedException e) { } 
	        catch (ExecutionException   e) { }         
	    }
	
	    executor.shutdown();     
	}
	
	
	public static void For(int start, int stop, final LoopBody loopBody)
	{
		Parallel.For(start, stop, 1, loopBody);   
	}
	
	public static void For(int start, int stop, int step, final LoopBody loopBody)
	{
		ExecutorService executor = Executors.newFixedThreadPool(iCPU);
		List<Future<?>> futures  = new LinkedList<Future<?>>();
		
		for (int i=start; i<stop; i+=step)
		{
			if (loopBody instanceof LoopBodyArgs<?>)
			{
				final Integer index = i;
				Future<?> future = executor.submit(new Runnable()
				{
					@SuppressWarnings("unchecked")
					public void run() { ((LoopBodyArgs<Integer>)loopBody).run(index); }
				});     
				
				futures.add(future);
			}
			else
			{
				Future<?> future = executor.submit(new Runnable()
				{
					public void run() { loopBody.run(); }
				});     
				
				futures.add(future);
			}
		}
			
		for (Future<?> f : futures)
		{
			try   { f.get(); }
		    catch (InterruptedException e) { } 
			catch (ExecutionException   e) { }         
		}
		
		executor.shutdown();     
	}
	
}