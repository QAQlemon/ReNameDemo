import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    //todo 输出根路径
    Path targetPath;
    //todo 当前选择的输出目录
    Path currentOutDir;

    //todo 编号参数 起始标号
    int start = 0;

    //todo 编号参数 增量控制
    int step = 1;



    //todo 自动根据输出目录中的已有文件进行文件续编
    boolean append = false;

    //todo 输出前排序处理
    Comparator<File> comparator = null;

    //todo
    boolean sortedByName=true;

    //todo 排序后反转处理
    boolean reversed=false;



    RenameBuilder builder=null;
    int fileNums=0;//检测到的文件数
    Thread runner=null;//工作线程

    public static class RenameBuilder{
        private Rename target;
//        private int step;
        RenameBuilder(){
            this.target = new Rename();
            this.target.builder=this;
//            step = 0;
        }
        //todo 路径设置
        public RenameBuilder init(String sourcePath,String targetPath) throws IOException {
            target.sourcePath = Paths.get(sourcePath);
            target.targetPath = Paths.get(targetPath);
            if(!Files.exists(target.sourcePath))
            {
                Path directory = Files.createDirectory(target.sourcePath);
                System.out.println(Thread.currentThread().getName()+"：创建源目录,"+directory);
            }
            if(!Files.exists(target.targetPath))
            {
                Path directory = Files.createDirectory(target.targetPath);
                System.out.println(Thread.currentThread().getName()+"：创建输出目录,"+directory);
            }
            target.currentOutDir=target.targetPath;
            return this;
        }

        //todo 手动设置起始编号
        public RenameBuilder  setStart(int start){
            target.start = start;
            return this;
        }
        //todo 自动根据已有文件进行续编
        public RenameBuilder setAppend(){
            target.append =true;
            return this;
        }
        //todo 自定义输出前排序处理
        public RenameBuilder preSort(Comparator<File> comparator){
            target.comparator = comparator;
            return this;
        }
        //
        public RenameBuilder sortByName(boolean flag){
            if(flag){
                //todo 按照文件夹默认排序方式
                if(target.reversed){
                    target.comparator=Comparator.reverseOrder();
                }else{
                    target.comparator=null;
                }
            }else{
                //todo 将文件名按照数值大小排列
                target.comparator=(f1, f2) -> {
                    int n1 = Integer.parseInt(f1.getName().split("\\.")[0]);
                    int n2 = Integer.parseInt(f2.getName().split("\\.")[0]);
                    if(target.reversed)
                    {
                        return Integer.compare(n2,n1);
                    }else {
                        return Integer.compare(n1,n2);
                    }
                };
            }
            target.sortedByName=flag;
            return this;
        }

        //todo 反转
//        public RenameBuilder reverse(){
//            reverse(true);
//            sortByName(target.sortedByName);
//            return this;
//        }
        public RenameBuilder reverse(boolean reverse){
            target.reversed = reverse;
            sortByName(target.sortedByName);
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
        System.out.println("新增文件: " + newFile);
        fileNums++;
    }
    public void changeParam(String input) throws IOException {
        Pattern pattern = Pattern.compile(".*[=\\d*]?");
        String[] args = input.split(" ");
        for (int i = 0; i < args.length; i++) {
            Matcher matcher = pattern.matcher(args[i]);
            //todo 检查是否匹配
            if(matcher.matches()){
                String[] param = matcher.group().split("=");
                String key = param[0];
                switch (key.intern()){
                    case "s":
                        if(param.length==2)
                        {
                            this.builder.sortByName(Boolean.parseBoolean(param[1]));
                        }else {
                            this.builder.sortByName(false);
                        }
                        break;
                    case "a":
                        this.append = param.length != 2 || Boolean.parseBoolean(param[1]);
                        break;
                    case "r":
                        if(
                            (param.length==2&&Boolean.parseBoolean(param[1]))
                            || param.length == 1
                        )
                        {
                            this.builder.reverse(true);
                            break;
                        }
                        this.builder.reverse(false);
                        break;
                    case "c":
                        if(args.length!=1){
                            break;
                        }
                        else {
                            changeCurrentOutDir(null);
                            break;
                        }
                    case "f":
                        if(param.length == 2)
                        {
                            changeCurrentOutDir(param[1]);
                        }
                        else {
                            changeCurrentOutDir(null);
                        }
                        break;
                    case "start":
                        if(param.length == 2)
                        {
                            this.builder.setStart(Integer.parseInt(param[1]));
                        }
                        break;
                    case "step":
                        if(param.length == 2) {
                            this.builder.step(Integer.parseInt(param[1]));
                        }
                        break;

                }
            }
        }
    }

    private void changeCurrentOutDir(String path) throws IOException {
        String input=null;
        Path outPath=null;
        if(path==null)
        {
            Scanner scanner = new Scanner(System.in);
            System.out.println(">选择输出目录：");
            Arrays.stream(targetPath.toFile().list()).forEach(System.out::println);
            System.out.print(Thread.currentThread().getName()+">输入或新建子目录：");
            input = scanner.nextLine();//todo 获取修改输出目录名
            outPath = Paths.get(this.targetPath.toString(), input);
        }else {
            outPath = Paths.get(this.targetPath.toString(), path);
        }
        if(!Files.exists(outPath))
        {
            Files.createDirectory(outPath);
            System.out.println("新建输出目录："+outPath.toString());
        }else {
            System.out.println("切换输出目录："+outPath.toString());
        }
        this.currentOutDir=outPath;

    }

    public boolean execute() throws IOException {

        Scanner scanner = new Scanner(System.in);
        long start = System.currentTimeMillis();
        do{
            System.out.println(Thread.currentThread().getName()+">是否开始编号处理：y/c/n,fileNums="+this.fileNums+",out="+this.currentOutDir);
            System.out.println(">start="+this.start+" step="+this.step+" sortedByName="+this.sortedByName+" reversed="+this.reversed);
            System.out.print(">");
            String input = scanner.nextLine();
            if(input.equals("y")){//todo 立即处理
                synchronized (this) {
                    System.out.println(Thread.currentThread().getName()+":notify");
                    fileNums=0;
                    this.notify();

                }
                return true;
            }else if(input.split(" ")[0].equals("c")){
                //todo 参数处理
                changeParam(input);
            }
            else {
                break;
            }
        }while (true);
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
//                    System.out.println(Thread.currentThread().getName()+":wait...");
                    this.wait();
//                    System.out.println(Thread.currentThread().getName()+":awake");
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
                    //todo 预排序处理
                    if(comparator!=null)
                        Arrays.asList(subFiles).sort(comparator);
//                    todo 反转处理
//                    if (reversed)
//                        Arrays.asList(subFiles).sort(comparator);

                    //todo 根据out目录最后一个文件名序号自动增长
                    if(append){
                        int startIndex=start;
                        File outDir = this.currentOutDir.toFile();
                        String[] fileNameList = outDir.list((dir, name) -> {
                            return !Files.isDirectory(Paths.get(dir.getPath(),name));
                        });

                        if(fileNameList.length != 0)
                        {
//                            String lastFileName = fileNameList[fileNameList.length - 1];
//                            String prefix = lastFileName.split("\\.")[0];
                            List<Integer> list = Arrays.stream(fileNameList).map(s -> Integer.parseInt(s.split("\\.")[0])).toList();
                            Optional<Integer> max = list.stream().max(Integer::compare);
                            start=max.get()+step;
                        }
                    }
                    for (int i = 0; i < subFiles.length; i++) {
                        file = subFiles[i];
                        sub_fix = file.getName().substring(file.getName().indexOf('.'));
                        try {
                            Files.move(file.toPath(), Paths.get(this.currentOutDir.toString(),(start+i*step)+sub_fix), StandardCopyOption.REPLACE_EXISTING);
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
