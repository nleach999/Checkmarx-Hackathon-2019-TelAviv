package com.checkmarx.plugin.updater.client.exceptions;


public class MisconfiguredException extends Exception
{

    private static final long serialVersionUID = -9029008444747778203L;

    

    public MisconfiguredException (String msg)
    {
        super (msg);
    }
}