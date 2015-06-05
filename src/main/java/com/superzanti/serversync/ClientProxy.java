package com.superzanti.serversync;

import com.superzanti.serversync.CommonProxy;

public class ClientProxy extends CommonProxy {

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public boolean isServer() {
        return false;
    }

}