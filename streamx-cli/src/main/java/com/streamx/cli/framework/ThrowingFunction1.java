package com.streamx.cli.framework;

@FunctionalInterface
public interface ThrowingFunction1<InputT, ResultT, ExceptionT extends Throwable> {
  ResultT get(InputT input) throws ExceptionT;
}