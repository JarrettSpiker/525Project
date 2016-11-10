package com.jspiker.accesscontrolsystem;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Created by jspiker on 10/31/16.
 */
public class Threading {

    private final static ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(50));



    public static <O> ListenableFuture<O> runOnBackgroundThread(final Function<Void, O> function){
        return service.submit(new Callable<O>() {
            @Override
            public O call() throws Exception {
                return function.apply(null);
            }
        });
    }


    public static ListenableFuture<Void> switchToBackground(){
        return Threading.runOnBackgroundThread(new Function<Void, Void>() {
            @Override
            public Void apply(Void input) {
                return null;
            }
        });
    }
}
