package com.heima.infrastructure.location;

import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class IpLocationService {
    private final Searcher searcher;
    public IpLocationService(@Value("${wanjuan.ip2region.xdb-path:}") String path) {
        Searcher created=null;
        try { if (path!=null && !path.isBlank() && Files.isRegularFile(Path.of(path))) created=Searcher.newWithBuffer(Files.readAllBytes(Path.of(path))); }
        catch (Exception ignored) { }
        this.searcher=created;
    }
    public String locate(String ip) {
        if (searcher==null || ip==null || ip.isBlank()) return "未知";
        try { String region=searcher.search(ip); return region==null || region.isBlank() ? "未知" : region.replace("|0","").replace('0',' ').replace('|',' ').trim(); }
        catch (Exception e) { return "未知"; }
    }
    @PreDestroy void close() { if (searcher!=null) try { searcher.close(); } catch (Exception ignored) {} }
}
