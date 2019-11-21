package com.boot.netty.rpc.protocal;

import lombok.Data;

@Data
public class RpcRequest {

    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] paramterTypes;

    private Object[] paramters;
}
