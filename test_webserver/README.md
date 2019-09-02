# Local Testing Web Server

This is a web server running in a Docker container to test update resolution and downloads.  It is required that you install [Docker desktop](https://www.docker.com/products/docker-desktop) if you do not already have it installed.

If you already have a web server running on your machine and it is using port 80, you will need to shutdown the local web server.  Docker will report that port 80 is already in use when attempting to run the testing web server if there is something already listening on port 80.

The empty folder `drop_plugin_update_files_here` is a spot where you can drop test files that match the regex spec of a plugin download payload.  Using the [Command Line Tester](../cmdline_tester), you can specify the default hostname as `localhost` and the resolution/download will execute against localhost.  You can alternatly update your hosts file to have the default hostname of `cxpluginupdate` resolve to your local test web server.

