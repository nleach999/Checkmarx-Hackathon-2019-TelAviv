using System;
using System.IO;
using System.ComponentModel.Design;
using System.Diagnostics;
using System.Globalization;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.Shell;
using Microsoft.VisualStudio.Shell.Interop;
using Task = System.Threading.Tasks.Task;
using System.IO.Compression;
using System.Windows.Forms;
using System.Reflection;
using System.Configuration;

namespace CXPluginUpdate
{
    
    /// <summary>
    /// Command handler
    /// </summary>
    internal sealed class UpdateCxPlugin
    {
        /// <summary>
        /// Command ID.
        /// </summary>
        /// 
        private const string  DOWNLOAD_FAILED= "Could not download latest plugin ";
        private const string MESSAGE_TITLE = "UpdateCxPlugin";
        private const string CHECK_FOR_LATESTVERSION = "checking for latest version, press ok to continue...";
        public const int CommandId = 0x0100;

        /// <summary>
        /// Command menu group (command set GUID).
        /// </summary>
        public static readonly Guid CommandSet = new Guid("98b99d43-0696-441b-93d3-03ae9cac14ad");

        /// <summary>
        /// VS Package that provides this command, not null.
        /// </summary>
        private readonly AsyncPackage package;

        /// <summary>
        /// Initializes a new instance of the <see cref="UpdateCxPlugin"/> class.
        /// Adds our command handlers for menu (commands must exist in the command table file)
        /// </summary>
        /// <param name="package">Owner package, not null.</param>
        /// <param name="commandService">Command service to add command to, not null.</param>
        private UpdateCxPlugin(AsyncPackage package, OleMenuCommandService commandService)
        {
            this.package = package ?? throw new ArgumentNullException(nameof(package));
            commandService = commandService ?? throw new ArgumentNullException(nameof(commandService));

            var menuCommandID = new CommandID(CommandSet, CommandId);
            var menuItem = new MenuCommand(this.Execute, menuCommandID);
            commandService.AddCommand(menuItem);
        }

        /// <summary>
        /// Gets the instance of the command.
        /// </summary>
        public static UpdateCxPlugin Instance
        {
            get;
            private set;
        }

        /// <summary>
        /// Gets the service provider from the owner package.
        /// </summary>
        private Microsoft.VisualStudio.Shell.IAsyncServiceProvider ServiceProvider
        {
            get
            {
                return this.package;
            }
        }

        /// <summary>
        /// Initializes the singleton instance of the command.
        /// </summary>
        /// <param name="package">Owner package, not null.</param>
        public static async Task InitializeAsync(AsyncPackage package)
        {
            // Switch to the main thread - the call to AddCommand in UpdateCxPlugin's constructor requires
            // the UI thread.
            await ThreadHelper.JoinableTaskFactory.SwitchToMainThreadAsync(package.DisposalToken);

            OleMenuCommandService commandService = await package.GetServiceAsync((typeof(IMenuCommandService))) as OleMenuCommandService;
            Instance = new UpdateCxPlugin(package, commandService);
        }

        /// <summary>
        /// This function is the callback used to execute the command when the menu item is clicked.
        /// See the constructor to see how the menu item is associated with this function using
        /// OleMenuCommandService service and MenuCommand class.
        /// </summary>
        /// <param name="sender">Event sender.</param>
        /// <param name="e">Event args.</param>
        private void Execute(object sender, EventArgs e)
        {
            ThreadHelper.ThrowIfNotOnUIThread();
            
            string Error_message = string.Format(CultureInfo.CurrentCulture, DOWNLOAD_FAILED, this.GetType().FullName);
            string title = MESSAGE_TITLE;


            //the  file path containing the JAR
            string executingassembly = Assembly.GetExecutingAssembly().Location;
            Configuration config = ConfigurationManager.OpenExeConfiguration(executingassembly);
            string Jar_filename = config.AppSettings.Settings["Jar_filename"].Value;

            string Jar_file_path = Path.Combine(Path.GetDirectoryName(executingassembly), Jar_filename);
             //the regex we wish to look for new plugin updates
            string additionalArgs = config.AppSettings.Settings["args"].Value;


            //start looking for updates
            Process process = new Process();
            //get the JAVA variable
           string Java_path =  Environment.GetEnvironmentVariable("JAVA_HOME");
           process.StartInfo.FileName = Path.Combine(Java_path,"bin","java.exe");

            //download to the temp folder project
            string Temp_folder = config.AppSettings.Settings["Tempfolder"].Value;

            string subdir = Temp_folder + Guid.NewGuid();


            // If directory does not exist, create it. 
            if (!Directory.Exists(subdir))
            {
                Directory.CreateDirectory(subdir);
            }

            process.StartInfo.WorkingDirectory = subdir;

            process.StartInfo.WindowStyle = ProcessWindowStyle.Hidden;

            process.StartInfo.Arguments = "-jar \""+ Jar_file_path +"\" " + additionalArgs;

            try
            {
                //start downloading plugin updates
                process.Start();
                string message = string.Empty;
                
                message = string.Format(CultureInfo.CurrentCulture, CHECK_FOR_LATESTVERSION, this.GetType().FullName);
                MessageBox.Show(message, "CXUpdater", MessageBoxButtons.OK);   

                process.WaitForExit();

                
                string latestversion= string.Empty;
                
                bool success = true;
                string[] files = Directory.GetFiles(subdir, "*.zip");

                //once a new plugin has been found then we install it
                foreach (string file in files)
                {
                    success = installPlugin(file, subdir ,config);

                    latestversion = Path.GetFileNameWithoutExtension(file).Substring(9);
                }
                
                
                if(success)
                  message = string.Format(CultureInfo.CurrentCulture, "Latest Plugin "+ latestversion + " downloaded and installed", this.GetType().FullName);
                else
                 message = string.Format(CultureInfo.CurrentCulture, "Latest Plugin installation failed", this.GetType().FullName);


                // Show a message box to prove we were here
                VsShellUtilities.ShowMessageBox(
                this.package,
                message,
                title,
                OLEMSGICON.OLEMSGICON_INFO,
                OLEMSGBUTTON.OLEMSGBUTTON_OK,
                OLEMSGDEFBUTTON.OLEMSGDEFBUTTON_FIRST);
            }
            catch 
            {
                string message = string.Format(CultureInfo.CurrentCulture, "Latest Plugin download failed", this.GetType().FullName);


                VsShellUtilities.ShowMessageBox(
                    this.package,
                    message,
                    title,
                    OLEMSGICON.OLEMSGICON_INFO,
                    OLEMSGBUTTON.OLEMSGBUTTON_OK,
                    OLEMSGDEFBUTTON.OLEMSGDEFBUTTON_FIRST);

            }
            

        }
        /// <summary>
        /// install the .vsix file will install or override the latest plugin installed
        /// </summary>
        /// <param name="file"></param>
        /// <param name="subdir"></param>
        /// <returns></returns>
        private bool installPlugin(string file,string subdir,Configuration config)
        {
            Process process = new Process();

            string message = string.Empty;

            process.StartInfo.FileName = config.AppSettings.Settings["VSIXInstaller"].Value;
            
            String ZipPath = file;
            String extractPath = subdir;

            try
            {
                ZipFile.ExtractToDirectory(ZipPath, extractPath);

                string LatestDownloadExtension = Getextension(extractPath); 

                process.StartInfo.Arguments = LatestDownloadExtension;
                process.StartInfo.WorkingDirectory = extractPath;



                message = string.Format(CultureInfo.CurrentCulture, "Installing latest version, press ok to continue...", this.GetType().FullName);
                MessageBox.Show(message, "CXUpdater", MessageBoxButtons.OK);

                process.Start();

                process.WaitForExit();

                return true;
            }
            catch(Exception ex)
            {

                return false;
            }

        }
        /// <summary>
        /// utility to get the .vsix file
        /// </summary>
        /// <param name="extractPath"></param>
        /// <returns></returns>
        private string Getextension(string extractPath)
        {
            string[] ExtensionFiles = Directory.GetFiles(extractPath, "*.vsix");
            return ExtensionFiles[0];
        }
    }

  
}
