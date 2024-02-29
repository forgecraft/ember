package net.forgecraft.services.ember.mods.downloader;

import net.forgecraft.services.ember.app.mods.downloader.CurseForgeDownloader;
import net.forgecraft.services.ember.app.mods.downloader.DownloaderFactory;
import net.forgecraft.services.ember.app.mods.downloader.MavenDownloader;
import net.forgecraft.services.ember.app.mods.downloader.ModrinthDownloader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DownloaderResolveTest {
    @Test
    public void correctlyResolvesToCurseforge() {
        var downloader = DownloaderFactory.INSTANCE.factory("https://www.curseforge.com/minecraft/mc-mods/jei");

        assertNotNull(downloader);
        assertInstanceOf(CurseForgeDownloader.class, downloader);
    }

    @Test
    public void correctlyResolveToCurseforceUsingLookup() {
        var downloader = DownloaderFactory.INSTANCE.factory("curseforge:100:100");

        assertNotNull(downloader);
        assertInstanceOf(CurseForgeDownloader.class, downloader);
    }

    @Test
    public void doesNotResolveToCurseforge() {
        var downloader = DownloaderFactory.INSTANCE.factory("https://www.notcurseforge.com/minecraft/mc-mods/jei");

        assertFalse(downloader instanceof CurseForgeDownloader);
    }

    @Test
    public void doesNotResolveToCurseforgeUsingLookup() {
        var downloader = DownloaderFactory.INSTANCE.factory("notcurseforge:100:100");

        assertFalse(downloader instanceof CurseForgeDownloader);
    }

    @Test
    public void correctlyResolvesToModrinth() {
        var downloader = DownloaderFactory.INSTANCE.factory("https://modrinth.com/mod/jei");

        assertNotNull(downloader);
        assertInstanceOf(ModrinthDownloader.class, downloader);
    }

    @Test
    public void correctlyResolveToModrinthUsingLookup() {
        var downloader = DownloaderFactory.INSTANCE.factory("modrinth:100:100");

        assertNotNull(downloader);
        assertInstanceOf(ModrinthDownloader.class, downloader);
    }

    @Test
    public void doesNotResolveToModrinth() {
        var downloader = DownloaderFactory.INSTANCE.factory("https://www.notmodrinth.com/minecraft/mc-mods/jei");

        assertFalse(downloader instanceof ModrinthDownloader);
    }

    @Test
    public void doesNotResolveToModrinthUsingLookup() {
        var downloader = DownloaderFactory.INSTANCE.factory("notmodrinth:100:100");

        assertFalse(downloader instanceof ModrinthDownloader);
    }

    @Test
    public void correctlyResolveToMaven() {
        var downloader = DownloaderFactory.INSTANCE.factory("maven:pro.mikey.mavendownloader:1.0.0");

        assertNotNull(downloader);
        assertInstanceOf(MavenDownloader.class, downloader);
    }

    @Test
    public void doesNotResolveToMaven() {
        var downloader = DownloaderFactory.INSTANCE.factory("https://www.notmaven.com/minecraft/mc-mods/jei");

        assertFalse(downloader instanceof MavenDownloader);
    }
}
