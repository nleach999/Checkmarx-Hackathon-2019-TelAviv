# Command Line Tester

This is a basic command line tester.  Run without arguments to see the available options.

To enable debugging in VSCode, here is a suggested launch config:


```
{
            "type": "java",
            "name": "CmdTest (with RegexName)",
            "request": "launch",
            "mainClass": "com.checkmarx.CmdTest",
            "projectName": "cmdline_tester",
            "args": [
                "-h",
                "localhost",
                "-regex-name",
                "extract-example",
                "-regex-props",
                "cmdline_tester/regex.properties",
                "-g",
                "filename",
                "major",
                "minor",
                "revision",
                "custom",
                "-t",
                "15"
            ]
        }


```