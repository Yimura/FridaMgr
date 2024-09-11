package sh.damon.fridamgr.models.github;

public class Release {
    public String tag_name;
    public String name;
    public boolean prerelease;
    public ReleaseAsset[] assets;
}
