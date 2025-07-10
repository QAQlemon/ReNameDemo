import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * @ClassName: CheckingDir
 * @Description:
 * @Author: zc
 * @Create: 2025/7/10 00:07
 * @Version: v1.0
 */
public class AutoCheckDir {
    public static void main(String[] args) throws IOException, InterruptedException {

//        File result = Paths.get("result/test2").toFile();
//        String[] list = result.list();
//        String[] list1 = result.list((f, name) -> {
//            return f.isDirectory();
//        });
//        String[] list2 = result.list((f, name) -> {
//            return !f.isDirectory();
//        });

        Rename rename = Rename.getBuilder()
                .init("renameDir", "result")
                .setStart(2050)
                .step(1)
                .setAppend()
//                .preSort()
                .sortByName(true)
//                .reverse()
                .build();
        rename.start();

        Path dir = Paths.get("renameDir");
        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("开始监控目录: " + dir);

        while (true) {
            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path newFile = (Path) event.context();
//                    System.out.println("新增文件: " + newFile);
                    rename.add(newFile);
                }
            }

            rename.execute();

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }

    }
}
