package com.checkmarx.plugin.updater.client.exceptions;



public class BadBuilderException extends Exception
{
    private static final long serialVersionUID = 8935236373282318263L;

    public BadBuilderException()
    {
        super ("Builder was invoked improperly.");
    }
}