package com.streamx.cli.framework;

@FunctionalInterface
public interface ThrowingFunction<ResultT, ExceptionT extends Throwable> {
  ResultT get() throws ExceptionT;
}