﻿using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Windows.Forms;
using Microsoft.Win32;
using mshtml;
using SHDocVw;
using System.IO;
using System.Runtime.InteropServices.Expando;
using System.Reflection;
using System.Net;
using System.Collections.Specialized;
using System.Text;

namespace ObjectSpyExtension
{
    [ComVisible(true),
     ClassInterface(ClassInterfaceType.None), 
     ProgId("KMS.qAutomate.ObjectSpyBHO.OjectSpy"),
     Guid("8CB0FB3A-8EFA-4F94-B605-F3427688F8C7"),
     ComDefaultInterface(typeof(IHttpRequestExtension))]
    public class ObjectSpyBHO : IObjectWithSite, IHttpRequestExtension
    {
        private object site;
        private IWebBrowser browser;
        private string serverUrl;
        private static String addonDataFolder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "KMS", "qAutomate", "ObjectSpy");
        private const string REQUEST_SEPARATOR = "_|_";

        #region Handle Document Events
        private void OnDocumentComplete(object pDisp, ref object URL)
        {
            if (pDisp == this.site)
            {
                return;
            }
            try
            {
                IWebBrowser2 childBrowser = GetBrowser(pDisp);
                if (childBrowser == null)
                {
                    return;
                }
                serverUrl = GetKatalonServerUrl();
                if (serverUrl != null)
                {
                    RunScriptOnDocument(childBrowser.Document as IHTMLDocument2);
                }
            }
            catch (Exception ex)
            {
                logError(ex);
            }
            
        }

        private void OnDownloadComplete()
        {
            IHTMLDocument2 doc = browser.Document as IHTMLDocument2;
            if (doc == null || doc.parentWindow == null)
            {
                return;
            }
            try
            {
                IHTMLWindow2 tmpWindow = doc.parentWindow;
                HTMLWindowEvents2_Event events = (tmpWindow as HTMLWindowEvents2_Event);
                if (events == null)
                {
                    return;
                }
                events.onload -= new HTMLWindowEvents2_onloadEventHandler(OnLoad);
                serverUrl = GetKatalonServerUrl();
                if (serverUrl != null)
                {
                    events.onload += new HTMLWindowEvents2_onloadEventHandler(OnLoad);
                }
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }

        public void OnLoad(IHTMLEventObj e)
        {
            try
            {
                RunScriptOnDocument(browser.Document as IHTMLDocument2);
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }

        private void RunScriptOnDocument(IHTMLDocument2 document)
        {
            IHTMLWindow2 window = document.parentWindow;
            RunScriptOnWindow(window, this);
        }

        private void RunScriptOnWindow(IHTMLWindow2 window, IHttpRequestExtension extensionClass)
        {
            IExpando windowEx = (IExpando) window;
            PropertyInfo propertyInfo = null;
            try
            {
                propertyInfo = windowEx.GetProperty("httpRequestExtension", System.Reflection.BindingFlags.IgnoreCase);
                if (propertyInfo == null)
                {
                    propertyInfo = windowEx.AddProperty("httpRequestExtension");
                }
            }
            catch (AmbiguousMatchException)
            {
                // Ambiguous match error, ignored
                propertyInfo = windowEx.AddProperty("httpRequestExtension");
            }
            propertyInfo.SetValue(windowEx, extensionClass, null); 

            window.execScript(Properties.Resources.jquery_1_11_2_min);
            window.execScript(Properties.Resources.jquery_color);
            window.execScript(Properties.Resources.json3_min);
            window.execScript(Properties.Resources.wgxpath_install);
            window.execScript(Properties.Resources.process_element);
            window.execScript("qAutomate_server_url = '" + serverUrl + "';");
            window.execScript(Properties.Resources.constants);
            window.execScript(Properties.Resources.common);
            window.execScript(Properties.Resources.dom_inspector);
            window.execScript(Properties.Resources.dom_collector);
            window.execScript(Properties.Resources.main);
        }

        #endregion

        #region Implementation of IObjectWithSite
        [Guid("6D5140C1-7436-11CE-8034-00AA006009FA")]
        [InterfaceType(1)]
        public interface IServiceProvider
        {
            int QueryService(ref Guid guidService, ref Guid riid, out IntPtr ppvObject);
        }

        private IWebBrowser2 GetBrowser(object site)
        {
            if (site == null || !(site is IServiceProvider))
            {
                return null;
            }
            var serviceProv = (IServiceProvider)site;
            IntPtr intPtr;
            Guid guidIWebBrowserApp = Marshal.GenerateGuidForType(typeof(IWebBrowserApp)); 
            Guid guidIWebBrowser2 = Marshal.GenerateGuidForType(typeof(IWebBrowser2));
            serviceProv.QueryService(ref guidIWebBrowserApp, ref guidIWebBrowser2, out intPtr);
            object result = Marshal.GetObjectForIUnknown(intPtr);
            if (result is IWebBrowser2)
            {
                return (IWebBrowser2)result;
            }
            return null;
        }

        int IObjectWithSite.SetSite(object site)
        {

            if (site == null)
            {
                Dispose();
                browser = null;
                return 0;
            }
            try
            {
                this.site = site;
                browser = GetBrowser(site);
                Setup();
            }
            catch (Exception ex)
            {
                logError(ex);
            }
            return 0;
        }

        private void logError(Exception e)
        {
            if (e == null)
            {
                return;
            }
            try
            {
                if (!Directory.Exists(addonDataFolder))
                {
                    Directory.CreateDirectory(addonDataFolder);
                }
                long currentMilis = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
                string newLogFile = Path.Combine(addonDataFolder, currentMilis.ToString() + ".log");
                File.WriteAllText(newLogFile, e.ToString());
            }
            catch (Exception)
            {
                // IO Exceptions, ignored
            }
            
        }

        private string GetKatalonServerUrl()
        {
            if (!Directory.Exists(addonDataFolder))
            {
                return null;
            }
            string serverSettingFile = Path.Combine(addonDataFolder, "serverUrl.txt");
            if (!File.Exists(serverSettingFile))
            {
                return null;
            }
            string serverUrl = File.ReadAllText(serverSettingFile);
            try
            {
                WebRequest request = WebRequest.Create(serverUrl);
                HttpWebResponse response = (HttpWebResponse)request.GetResponse();
                if (response.StatusCode == HttpStatusCode.OK)
                {
                    return serverUrl;
                }
            }
            catch (Exception ex)
            {
                // server not available at all, for some reason
            }
            return null;
        }

        private void Dispose()
        {
            ((DWebBrowserEvents2_Event)browser).DocumentComplete -=
                new DWebBrowserEvents2_DocumentCompleteEventHandler(this.OnDocumentComplete);
            ((DWebBrowserEvents2_Event)browser).DownloadComplete -=
                new DWebBrowserEvents2_DownloadCompleteEventHandler(this.OnDownloadComplete);
        }

        private void Setup()
        {
            ((DWebBrowserEvents2_Event)browser).DocumentComplete +=
                new DWebBrowserEvents2_DocumentCompleteEventHandler(this.OnDocumentComplete);
            ((DWebBrowserEvents2_Event)browser).DownloadComplete +=
                new DWebBrowserEvents2_DownloadCompleteEventHandler(this.OnDownloadComplete);
        }

        int IObjectWithSite.GetSite(ref Guid guid, out IntPtr ppvSite)
        {
            try
            {
                IntPtr punk = Marshal.GetIUnknownForObject(browser);
                int hr = Marshal.QueryInterface(punk, ref guid, out ppvSite);
                Marshal.Release(punk);
                return hr;
            }
            catch (Exception ex)
            {
                logError(ex);
                throw ex;
            }
        }
        #endregion

        #region Registering with regasm
        public static string RegBHO = "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Browser Helper Objects";

        [ComRegisterFunction]
        public static void RegisterBHO(Type type)
        {
            string guid = type.GUID.ToString("B");
            RegistryKey registryKey = Registry.LocalMachine.OpenSubKey(RegBHO, true);
            if (registryKey == null)
            {
                registryKey = Registry.LocalMachine.CreateSubKey(RegBHO);
            }
            RegistryKey key = registryKey.OpenSubKey(guid);
            if (key == null)
            {
                key = registryKey.CreateSubKey(guid);
            }
            key.SetValue("Alright", 1);
            registryKey.Close();
            key.Close();
        }

        [ComUnregisterFunction]
        public static void UnregisterBHO(Type type)
        {
            string guid = type.GUID.ToString("B");
            RegistryKey registryKey = Registry.LocalMachine.OpenSubKey(RegBHO, true);
            if (registryKey != null)
            {
                registryKey.DeleteSubKey(guid, false);
            }
        }
        #endregion

        #region Handle web request
        public String postRequest(string data, string url)
        {
            using (var client = new WebClient())
            {
                try
                {
                    client.UploadString(url, data);
                    return "200";
                }
                catch (WebException webException)
                {
                    if (webException.Response is HttpWebResponse)
                    {
                        HttpWebResponse response = (HttpWebResponse) webException.Response;
                        return "HTTP Status Code: " + (int)(response.StatusCode);
                    }
                    if (webException.Message.Contains("Unable to connect to the remote server"))
                    {
                        return "Cannot connect to Katalon Server. Make sure you have started Object Spy on Katalon application.";
                    }
                    return "Web Exception Error: " + webException.Message;
                }
                catch (Exception ex)
                {
                    logError(ex);
                    return "Internal Error: " + ex.ToString();
                }
            }
        }
        #endregion
        
        public String sendRequestToKatalon(string data, string url)
        {
            int statusCode = 200;
            String message = "";
            using (WebClient client = new WebClient())
            {
                try
                {
                    message = client.UploadString(url, data);
                }
                catch (WebException webException)
                {
                    HttpWebResponse response = (HttpWebResponse)webException.Response;
                    statusCode = (int)(response.StatusCode);
                    message = "Failed to connect to Katalon Server.";
                }
                catch (Exception ex)
                {
                    logError(ex);
                    statusCode = 500;
                    message = "Internal Server Error";
                }
                message = statusCode + REQUEST_SEPARATOR + message;
            }
            return message;
        }
    }
}