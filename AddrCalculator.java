/**
 * @ClassName: AddrCalculator
 * @Description:
 * @Author: zc
 * @Create: 2025/4/2 15:38
 * @Version: v1.0
 */
public class AddrCalculator {
    public static void main(String[] args) {
        //todo 控制参数
        int nums = 13;//数量
        int addr = 0x5410;//起始地址
        int size = 2*0x20;//区块大小
        for (int i = 0; i < nums; i++) {
            addr += size/2;
            System.out.printf("%04x\n",addr);

        }
    }
}
