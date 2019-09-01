docker build -t local_webserver container
docker run -d -v ${pwd}\drop_plugin_update_files_here:/var/plugins -p 80:80 --name web local_webserver
Write-Host ----: WEB SERVER IS RUNNING ON PORT 80.
Write-Host ----: Use 'kill_webserver.ps1' to stop the web server.


