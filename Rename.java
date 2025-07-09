import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: Rename
 * @Description:
 * @Author: zc
 * @Create: 2025/3/11 14:08
 * @Version: v1.0
 */
public class Rename {
    public static BasicFileAttributes readFileLastAccessTime(File file) throws IOException {
        BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return basicFileAttributes;
    }
    public static void main(String[] args) throws IOException {
        System.out.println("test");

        //todo 待处理的目录路径 ./
        File directory = Paths.get("renameDir").toFile();

        //todo 控制参数
        boolean autoIncrease = false;
        autoIncrease=true;//todo 自动增长

        boolean sortByTime =false;//默认按名称进行排序
        sortByTime=true;//todo 根据时间进行排序

        int start = 2000;//todo 编号参数 起始标号
        int step = 1;    //todo 编号参数 增量控制

        boolean reverse = false;
//        reverse=true;//todo 反转控制



        File file=null;
        String sub_fix=null;
        if (directory.exists() && directory.isDirectory()) {
            //todo 过滤文件夹
            File[] subFiles = directory.listFiles((dir, name) -> {
                return !Files.isDirectory(Paths.get(dir.getPath(),name));
            });
            //todo 按最后访问时间排序
            if(sortByTime)
                Arrays.asList(subFiles).sort((f1, f2) -> {
                    FileTime t1=null;
                    BasicFileAttributes a1=null;
                    FileTime t2=null;
                    BasicFileAttributes a2=null;
                    try {
//                        FileTime t1 = readFileLastAccessTime(f1);
//                        FileTime t2 = readFileLastAccessTime(f2);
                        a1 = readFileLastAccessTime(f1);
                        a2 = readFileLastAccessTime(f2);
                        t1=a1.lastAccessTime();
                        t2=a2.lastAccessTime();
                        return t1.compareTo(t2);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            //todo 反转处理
            if (reverse == true)
                Arrays.asList(subFiles).sort(Comparator.reverseOrder());

            //todo 根据out目录最后一个文件名序号自动增长
            if(autoIncrease){
                int startIndex=start;
                File outDir = Paths.get("./renameDir/out").toFile();
                String[] fileNameList = outDir.list();
                if(fileNameList.length != 0)
                {
                    String lastFileName = fileNameList[fileNameList.length - 1];
                    String prefix = lastFileName.split("\\.")[0];
                    start=Integer.parseInt(prefix)+1;
                }
                for (int i = 0; i < subFiles.length; i++) {
                    file = subFiles[i];
                    sub_fix = file.getName().substring(file.getName().indexOf('.'));
                    Files.move(file.toPath(), Paths.get("./renameDir/out",(start+i*step)+sub_fix), StandardCopyOption.REPLACE_EXISTING);
                }

            }else {
                if (subFiles != null) {
                    for (int i = 0; i < subFiles.length; i++) {
                        file = subFiles[i];
                        sub_fix = file.getName().substring(file.getName().indexOf('.'));
                        Files.move(file.toPath(), Paths.get("./renameDir/out",(start+i*step)+sub_fix), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }else{
            System.out.println("指定的路径不是一个有效的目录。");
        }
    }
}
