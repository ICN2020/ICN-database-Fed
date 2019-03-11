package com.ogb.fes.concurrent;


public interface LoopBodyArgs <T> extends LoopBody
{
    public void run(T i);
}