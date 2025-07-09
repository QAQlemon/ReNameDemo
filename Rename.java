import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: Rename
 * @Description:
 * @Author: zc
 * @Create: 2025/3/11 14:08
 * @Version: v1.0
 */

public class Rename implements Runnable {
    //todo 待处理的目录路径 ./
    Path sourcePath;
    //todo 输出路径
    Path targetPath;

    //todo 编号参数 起始标号
    int start = 0;

    //todo 编号参数 增量控制
    int step = 1;

    //todo 反转控制
    boolean reverse = false;

    //todo 自动根据输出目录中的已有文件进行文件续编
    boolean autoIncrease = false;

    //todo 输出前排序处理
    Comparator<File> comparator = null;

    int fileNums=0;//检测到的文件数
    Thread runner;

    public static class RenameBuilder{
        private Rename target;
        private int step;
        RenameBuilder(){
            this.target = new Rename();
            step = 0;
        }
        //todo 路径设置
        public RenameBuilder init(String sourcePath,String targetPath){
            target.sourcePath = Paths.get(sourcePath);
            target.targetPath = Paths.get(targetPath);
            return this;
        }

        //todo 手动设置起始编号
        public RenameBuilder  setStart(int start){
            target.start = start;
            return this;
        }
        //todo 自动根据已有文件进行续编
        public RenameBuilder  setAutoIncrease(){
            target.autoIncrease=true;
            return this;
        }
        //todo 输出前排序处理
        public RenameBuilder preSort(Comparator<File> comparator){
            target.comparator = comparator;
            return this;
        }
        //todo 反转
        public RenameBuilder reverse(){
            preSort(Comparator.reverseOrder());
            return this;
        }
        //todo 增长值
        public RenameBuilder step(int step){
            target.step = step;
            return this;
        }

        Rename build(){
            return this.target;
        }
    }

    public static BasicFileAttributes readFileLastAccessTime(File file) throws IOException {
        BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        return basicFileAttributes;
    }

    public void add(Path newFile){
        fileNums++;
    }
    public boolean execute(){

        Scanner scanner = new Scanner(System.in);
        System.out.println(Thread.currentThread().getName()+">是否开始编号处理：y/n");
        long start = System.currentTimeMillis();
        while(scanner.hasNext()){
//            System.out.println(System.currentTimeMillis() - start);
            if(System.currentTimeMillis() - start < 5000){
                String input = scanner.nextLine();
                if(input.equals("y")){//todo 立即处理
                    synchronized (this) {
                        System.out.println(Thread.currentThread().getName()+":notify");
                        this.notify();
                    }
                    return true;
                }else {
                    System.out.println("等待下次处理");
                    return false;
                }
            }else{
                break;
            }

        }
        System.out.println("等待下次处理");
        return false;
    }
    public void start(){
        //todo 处理线程
        Thread thread = new Thread(this);
        thread.start();

//        //todo 交互线程
//        new
    }
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    System.out.println(Thread.currentThread().getName()+":wait...");
                    this.wait();
                    System.out.println(Thread.currentThread().getName()+":awake");
                }

                //todo 待处理的目录路径 ./
                File directory = this.sourcePath.toFile();

                File file=null;
                String sub_fix=null;
                if (directory.exists() && directory.isDirectory()) {
                    //todo 过滤文件夹
                    File[] subFiles = directory.listFiles((dir, name) -> {
                        return !Files.isDirectory(Paths.get(dir.getPath(),name));
                    });
                    if (subFiles == null) {
                        return;
                    }

                    //todo 反转处理
                    if (this.comparator != null)
                        Arrays.asList(subFiles).sort(this.comparator);

                    //todo 根据out目录最后一个文件名序号自动增长
                    if(autoIncrease){
                        int startIndex=start;
                        File outDir = this.targetPath.toFile();
                        String[] fileNameList = outDir.list();
                        if(fileNameList.length != 0)
                        {
                            String lastFileName = fileNameList[fileNameList.length - 1];
                            String prefix = lastFileName.split("\\.")[0];
                            start=Integer.parseInt(prefix)+1;
                        }
                    }
                    for (int i = 0; i < subFiles.length; i++) {
                        file = subFiles[i];
                        sub_fix = file.getName().substring(file.getName().indexOf('.'));
                        try {
                            Files.move(file.toPath(), Paths.get(this.targetPath.toString(),(start+i*step)+sub_fix), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }else{
                    System.out.println("指定的路径不是一个有效的目录。");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }


    static RenameBuilder getBuilder(){
        return new RenameBuilder();
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
