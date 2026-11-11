package local.portal.pd;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import org.teavm.vm.TeaVMOptimizationLevel;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;

public final class BuildWeb {
    public static void main(String[] args) throws Exception {
        File out=new File("web/build/dist");
        new TeaCompiler(new WebBackend().setHtmlTitle("破碎的像素地牢").setStartJettyAfterBuild(false))
            .addAssets(new AssetFileHandle("core/src/main/assets"))
            .addAssets(new AssetFileHandle("desktop/src/main/assets"))
            .addReflectionClass("com.shatteredpixel.shatteredpixeldungeon.**")
            .addReflectionClass("com.watabou.**")
            .setMainClass(Launcher.class.getName()).setOutputName("dungeon")
            .setDebugInformationGenerated(false).setSourceMapsFileGenerated(false)
            .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE).setObfuscated(false).build(out);
        File js=new File(out,"webapp/dungeon.js");
        Files.writeString(js.toPath(),fixCompilerInitialization(Files.readString(js.toPath())),StandardCharsets.UTF_8);
        JSONObject provenance=new JSONObject(Files.readString(new File("web/upstream.json").toPath()));
        provenance.put("version",WebVersion.NAME);provenance.put("code",WebVersion.CODE);provenance.put("adapter",1);
        Files.writeString(new File(out,"webapp/version.json").toPath(),provenance.toString(2),StandardCharsets.UTF_8);
    }
    // TeaVM 0.14 reflection can invoke a captured static initializer twice. Keep its two entry points synchronized.
    // This pinned workaround is also documented by broqdev's Web port; reject a changed compiler runtime.
    static String fixCompilerInitialization(String source){
        String from="$rt_eraseClinit = target => target.$clinit = () => {";
        int at=source.indexOf(from);
        if(at<0||source.indexOf(from,at+from.length())>=0)throw new IllegalStateException("Review TeaVM static-initialization compatibility before release");
        return source.substring(0,at)+"$rt_eraseClinit = target => target[$rt_meta].clinit = target.$clinit = () => {"+source.substring(at+from.length());
    }
}
