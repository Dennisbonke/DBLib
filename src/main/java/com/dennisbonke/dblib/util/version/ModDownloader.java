package com.dennisbonke.dblib.util.version;

import com.dennisbonke.dblib.util.LogHelper;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class ModDownloader extends Thread{
    private static final File modsfolder = MinecraftServer.getServer().getFile("mods");
    private final File modFile;
    public final String modname, url;
    private final long size;

    public ModDownloader(String modName, String url, long size) {
        this.modname = modName;
        this.url = url;
        this.size = size;
        this.modFile = new File(modsfolder, FilenameUtils.getName(url));
        this.setName("Mod Downloader");
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            downloadFile(new URL(url), modFile, size);
            } catch (Exception e) {
            LogHelper.error("Could not download update!");
            //LogHelper.logException(e);
        }
    }

    public boolean downloadFile(URL par1URL, File par2File, long size) throws IOException {
        if (par2File.exists()) {
            if (par2File.length() == size)
                return false;
        } else if (!par2File.getParentFile().exists())
            par2File.getParentFile().mkdirs();
        byte[] var5 = new byte[4096];
        URLConnection con = par1URL.openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(15000);
        DataInputStream var6 = new DataInputStream(con.getInputStream());
        DataOutputStream var7 = new DataOutputStream(new FileOutputStream(par2File));
        while (true) {
            int var9;
            if ((var9 = var6.read(var5)) < 0) {
                var6.close();
                var7.close();
                return true;
            }
            var7.write(var5, 0, var9);
        }
    }
}
