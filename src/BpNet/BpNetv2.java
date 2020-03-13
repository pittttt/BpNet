package BpNet;

import java.awt.Font;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class BpNetv2 extends ApplicationFrame {
	private static final int IM = 2; // 输入层数量(包含一个偏置输入)
	private static final int RM1 = 64; // 隐含层1节点数量
	private static final int RM2 = 32; // 隐含层2节点数量
	private static final int OM = 1; // 输出层数量
	private static double learnRate = 0.001; // 学习速率
	private static double alfa = 0.001; // 动量因子

	private static double Win1[][] = new double[IM][RM1]; // 输入到隐含1连接权值
	private static double oldWin1[][] = new double[IM][RM1]; // 更新前上一次隐含1连接权值
	private static double old1Win1[][] = new double[IM][RM1]; // 上上一次隐含1连接权值
	private static double dWin1[][] = new double[IM][RM1]; // 连接权值的修正值△
	private static double Wout1[][] = new double[RM1][RM2]; // 隐含1到隐含2连接权值
	private static double oldWout1[][] = new double[RM1][RM2]; // 更新前上一次隐含1到隐含2连接权值
	private static double old1Wout1[][] = new double[RM1][RM2]; // 上上一次隐含1到隐含2连接权值
	private static double dWout1[][] = new double[RM1][RM2]; // 连接权值的修正值△

	// private static double Win2[][] = new double[RM1][RM2]; //隐含1到隐含2连接权值
	// private static double oldWin2[][] = new double[RM1][RM2]; //更新前上一次隐含2连接权值
	// private static double old1Win2[][] = new double[RM1][RM2]; //上上一次隐含2连接权值
	// private static double dWin2[][] = new double[RM1][RM2]; //连接权值的修正值△
	private static double Wout2[][] = new double[RM2][OM]; // 隐含2到输出连接权值
	private static double oldWout2[][] = new double[RM2][OM]; // 更新前上一次隐含2到输出连接权值
	private static double old1Wout2[][] = new double[RM2][OM]; // 上上一次隐含2到输出连接权值
	private static double dWout2[][] = new double[RM2][OM]; // 连接权值的修正值△

	private static double Xi[] = new double[IM]; // 输入层输入
	private static double Xj1[] = new double[RM1]; // 隐含层1输出(未经过激活函数激活)
	private static double XjActive1[] = new double[RM1]; // 隐含层1输出(激活后)
	private static double Xj2[] = new double[RM2]; // 隐含层2输出(未经过激活函数激活)
	private static double XjActive2[] = new double[RM2]; // 隐含层2输出(激活后)
	private static double Xk[] = new double[OM]; // 真实输出(输出层无激活函数)
	private static double Ek[] = new double[OM]; // 误差
	private static double J = 0.1; // 误差性能指标
	static int[][] data = new int[3651][5]; // 存放训练数据
	static long startTime;
	static double loadData[] = new double[2]; // 输入数据
	static double outData[] = new double[1]; // 输出数据

	static double pData[] = new double[2000];

	public static void main(String[] arg) {
		startTime = System.currentTimeMillis(); // 或得程序开始运行时间
		// BpNet bpNet = new BpNet();

		// 训练数据导入及训练
		train();

		// 预测输出
		System.out.println("预测输出为:");
		for (int n = 3000; n < 3651; n++) {
			double y = data[n][1]; // 训练数据样本
			System.out.printf("%.1f  ", y); // 真实数据，用于对比误差
			System.out.printf("%f  ", bpNetOut(loadData(n))[0] * 100.0);// 此时为单个输出，打印的值为Uk[0]
			double w = ((y - (bpNetOut(loadData(n))[0] * 100.0)) / y) * 100;
			System.out.print("误差为:");
			System.out.printf("%.5f", w);
			System.out.println(" %");
		}
		// 为图表设置字体
		StandardChartTheme standardChartTheme = new StandardChartTheme("name");
		standardChartTheme.setLargeFont(new Font("宋体", Font.BOLD, 12));// 可以改变轴向的字体
		standardChartTheme.setRegularFont(new Font("宋体", Font.BOLD, 8));// 可以改变图例的字体
		standardChartTheme.setExtraLargeFont(new Font("宋体", Font.BOLD, 20));// 可以改变图表的标题字体
		ChartFactory.setChartTheme(standardChartTheme);// 设置主题
		// 绘制图表
		BpNetv2 fjc = new BpNetv2("销量预测图");
		fjc.pack();
		RefineryUtilities.centerFrameOnScreen(fjc);
		fjc.setBounds(600, 100, 900, 700); // 设置窗体大小
		fjc.setVisible(true);

		// Scanner keyboard = new Scanner(System.in);
		// System.out.println("Please enter the parameter of input:");
		// double parameter;
		// while((parameter = keyboard.nextDouble()) != -1)
		// System.out.println(parameter + "*2+23=" + bpNet.bpNetOut(parameter /
		// 100.0)[0] * 100.0);

	}

	// 样本数据输入
	private static double[] loadData(int n) {
		loadData[0] = (double) data[n][0];
		loadData[1] = n;
		return loadData;
	}

	// 预测数据输入
	private static double[] pData(int n) {
		pData[0] = 1;
		pData[1] = n;
		return pData;
	}

	private static void train() {
		XSSFWorkbook workbook = null;
		try {
			// 读取Excel文件
			InputStream inputStream = new FileInputStream("C:\\Users\\pitt\\Desktop\\temperatures.xlsx");
			workbook = new XSSFWorkbook(inputStream);
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 遍历Excel Sheet
		for (int numSheet = 0; numSheet < workbook.getNumberOfSheets(); numSheet++) {

			XSSFSheet sheet = workbook.getSheetAt(numSheet);
			Row row = null;
			int lastRowNum = sheet.getLastRowNum();
			// 循环读取
			// 循环行,从excel第二行开始，第一行为表头
			for (int i = 1; i < lastRowNum + 1; i++) {
				// 将偏置值赋给输入
				data[i][0] = 1;
				row = sheet.getRow(i);
				if (row != null) {
					// 获取每一列的值
					// 循环列，从第二列开始
					for (int j = 1; j < row.getLastCellNum(); j++) {
						Cell cell = row.getCell(j);
						if (cell != null) {
							int value = (int) cell.getNumericCellValue();
							data[i][j] = value;

							// System.out.println(data[i][j]);
						}
					}
				}
			}
			System.out.println("训练数据载入成功...");
		}
		double y;
		int n = 0;
		int s = 0;
		// 初始化权值和清零
		bpNetinit();
		System.out.println("training...");

		for (s = 0; s < 1000; s++) // 进行循环训练
		{
			for (n = 1; n < 3000; n++) {

				y = data[n][1]; // 训练数据样本
				loadData(n);
				outData[0] = y;
				// 前向计算输出过程
				bpNetForwardProcess(loadData, outData);
				// 反向学习修改权值
				bpNetReturnProcess();
			}
		}
		// 在线学习后输出
		for (n = 1; n < 30; n++) {
			y = data[n][1]; // 训练数据样本
			System.out.printf("%.1f  ", y);
			System.out.printf("%f", bpNetOut(pData(n))[0] * 100.0);// 此时为单个输出，打印的值为Uk[0]
			System.out.println();
			// System.out.println("J=" + J);
		}
		System.out.print("训练次数为:" + s + "次   ");
		System.out.println("训练所花费时间为：" + (System.currentTimeMillis() - startTime) * 1.0 / 1000 + " s");

		// int no=45;
		// System.out.println("n=45 " + "真实值为:"+(no * 2 + 23) +" 程序输出为:" +
		// this.bpNetOut(45 /100.0)[0] * 100);
	}

	//
	// BP神经网络权值随机初始化
	// Win[i][j]和Wout[j][k]权值初始化为[-0.5,0.5]之间
	//
	public static void bpNetinit() {
		// 初始化权值和清零
		for (int i = 0; i < IM; i++)
			for (int j = 0; j < RM1; j++) {
				Win1[i][j] = 0.5 - Math.random();
				Xj1[j] = 0;
			}
		for (int j = 0; j < RM1; j++)
			for (int k = 0; k < RM2; k++) {
				Wout1[j][k] = 0.5 - Math.random();
				Xj2[k] = 0;
			}
		for (int j = 0; j < RM2; j++)
			for (int k = 0; k < OM; k++) {
				Wout2[j][k] = 0.5 - Math.random();
				Xk[k] = 0;
			}
	}

	//
	// BP神经网络前向计算输出过程
	// @param inputParameter 归一化后的理想输入值(单个double值)
	// @param outputParameter 归一化后的理想输出值(单个double值)
	//
	// public static void bpNetForwardProcess(double inputParameter, double
	// outputParameter)
	// {
	// double input[] = {inputParameter};
	// double output[] = {outputParameter};
	// bpNetForwardProcess(input, output);
	// }

	//
	// BP神经网络前向计算输出过程--多个输入，多个输出
	// @param inputParameter 归一化后的理想输入数组值
	// @param outputParameter 归一化后的理想输出数组值
	//
	public static void bpNetForwardProcess(double inputParameter[], double outputParameter[]) {
		for (int i = 0; i < IM; i++) {
			Xi[i] = inputParameter[i] / 100;
		}
		// 隐含层1权值和计算//
		for (int j = 0; j < RM1; j++) {
			Xj1[j] = 0;
			for (int i = 0; i < IM; i++) {
				Xj1[j] = Xj1[j] + Xi[i] * Win1[i][j];
			}
		}
		// 隐含层1S激活输出//
		for (int j = 0; j < RM1; j++) {
			XjActive1[j] = 1 / (1 + Math.exp(-Xj1[j]));
		}

		// 隐含层2权值和计算//
		for (int j = 0; j < RM2; j++) {
			Xj2[j] = 0;
			for (int i = 0; i < RM1; i++) {
				Xj2[j] = Xj2[j] + XjActive1[i] * Wout1[i][j];
			}
		}
		// 隐含层2S激活输出//
		for (int j = 0; j < RM2; j++) {
			XjActive2[j] = 1 / (1 + Math.exp(-Xj2[j]));
		}
		// 输出层权值和计算//
		for (int k = 0; k < OM; k++) {
			Xk[k] = 0;
			for (int j = 0; j < RM2; j++) {
				Xk[k] = Xk[k] + XjActive2[j] * Wout2[j][k];
			}
		}
		// 计算输出与理想输出的偏差//
		for (int k = 0; k < OM; k++) {
			Ek[k] = outputParameter[k] / 100 - Xk[k];
		}

		// 误差性能指标//
		J = 0;
		for (int k = 0; k < OM; k++) {
			J = J + Ek[k] * Ek[k] / 2.0;
		}
	}

	//
	// BP神经网络反向学习修改连接权值过程
	//
	public static void bpNetReturnProcess() {
		// 反向学习修改权值//
		for (int i = 0; i < IM; i++) // 输入到隐含1权值修正
		{
			for (int j = 0; j < RM1; j++) {
				for (int k = 0; k < RM2; k++) {
					for (int l = 0; l < OM; l++) {
						dWin1[i][j] = dWin1[i][j] + learnRate
								* (Ek[l] * Wout1[j][k] * Wout2[k][l] * XjActive1[j] * (1 - XjActive1[j]) * Xi[i]);
					}
				}
				Win1[i][j] = Win1[i][j] + dWin1[i][j] + alfa * (oldWin1[i][j] - old1Win1[i][j]);
				old1Win1[i][j] = oldWin1[i][j];
				oldWin1[i][j] = Win1[i][j];
			}
		}

		for (int i = 0; i < RM1; i++) // 隐含1到隐含2权值修正
		{
			for (int j = 0; j < RM2; j++) {
				for (int k = 0; k < OM; k++) {
					dWout1[i][j] = dWout1[i][j]
							+ learnRate * (Ek[k] * Wout2[j][k] * XjActive2[j] * (1 - XjActive2[j]) * XjActive1[i]);
				}
				Wout1[i][j] = Wout1[i][j] + dWout1[i][j] + alfa * (oldWout1[i][j] - old1Wout1[i][j]);
				old1Wout1[i][j] = oldWout1[i][j];
				oldWout1[i][j] = Wout1[i][j];
			}
		}

		for (int j = 0; j < RM2; j++) // 隐含2到输出权值修正
		{
			for (int k = 0; k < OM; k++) {
				dWout2[j][k] = learnRate * Ek[k] * XjActive2[j];
				Wout2[j][k] = Wout2[j][k] + dWout2[j][k] + alfa * (oldWout2[j][k] - old1Wout2[j][k]);
				old1Wout2[j][k] = oldWout2[j][k];
				oldWout2[j][k] = Wout2[j][k];
			}
		}
	}

	//
	// BP神经网络前向计算输出，训练结束后测试输出
	// @param inputParameter 测试的归一化后的输入值
	// @return 返回归一化后的BP神经网络输出值，需逆归一化
	//
	// public static double[] bpNetOut(double inputParameter)
	// {
	// //在线学习后输出//
	// for(int i = 0; i < IM-1; i++)
	// {
	// Xi[i] = inputParameter/100;
	// }
	// //隐含层权值和计算//
	// for(int j = 0; j < RM; j++)
	// {
	// Xj[j] = 0;
	// for(int i = 0; i < IM; i++)
	// {
	// Xj[j] = Xj[j] + Xi[i] * Win[i][j];
	// }
	// }
	// //隐含层S激活输出//
	// for(int j = 0; j < RM; j++)
	// {
	// XjActive[j] = 1 / (1 + Math.exp(-Xj[j]));
	// }
	// //输出层权值和计算//
	// double Uk[] = new double[OM];
	// for(int k = 0; k < OM; k++)
	// {
	// Xk[k] = 0;
	// for(int j = 0; j < RM; j++)
	// {
	// Xk[k] = Xk[k] + XjActive[j] * Wout[j][k];
	// Uk[k] = Xk[k];
	// }
	// }
	// return Uk;
	// }
	//
	// BP神经网络前向计算输出，训练结束后测试输出
	// @param inputParameter 测试的归一化后的输入数组
	// @return 返回归一化后的BP神经网络输出数组
	//
	public static double[] bpNetOut(double[] inputParameter) {
		// 在线学习后输出//
		for (int i = 0; i < IM; i++) {
			Xi[i] = inputParameter[i] / 100;
		}
		// 隐含层1权值和计算//
		for (int j = 0; j < RM1; j++) {
			Xj1[j] = 0;
			for (int i = 0; i < IM; i++) {
				Xj1[j] = Xj1[j] + Xi[i] * Win1[i][j];
			}
		}
		// 隐含层1S激活输出//
		for (int j = 0; j < RM1; j++) {
			XjActive1[j] = 1 / (1 + Math.exp(-Xj1[j]));
		}

		// 隐含层2权值和计算//
		for (int j = 0; j < RM2; j++) {
			Xj2[j] = 0;
			for (int i = 0; i < RM1; i++) {
				Xj2[j] = Xj2[j] + XjActive1[i] * Wout1[i][j];
			}
		}
		// 隐含层2S激活输出//
		for (int j = 0; j < RM2; j++) {
			XjActive2[j] = 1 / (1 + Math.exp(-Xj2[j]));
		}
		// 输出层权值和计算//
		double Uk[] = new double[OM];
		for (int k = 0; k < OM; k++) {
			Xk[k] = 0;
			for (int j = 0; j < RM2; j++) {
				Xk[k] = Xk[k] + XjActive2[j] * Wout2[j][k];
				Uk[k] = Xk[k];
			}
		}
		return Uk;
	}

	// 图表绘制
	private static final long serialVersionUID = 1L;

	public BpNetv2(String s) {
		super(s);
		setContentPane(createDemoLine());
	}

	// 生成显示图表的面板
	static JFreeChart jfreechart;

	public static JPanel createDemoLine() {
		jfreechart = createChart(createDataset());
		return new ChartPanel(jfreechart);
	}

	// 生成图表主对象JFreeChart
	public static JFreeChart createChart(DefaultCategoryDataset linedataset) {
		// 定义图表对象
		JFreeChart chart = ChartFactory.createLineChart("销量曲线", // 折线图名称
				"时间(周)", // 横坐标名称
				"销售量(台)", // 纵坐标名称
				linedataset, // 数据
				PlotOrientation.VERTICAL, // 水平显示图像
				true, // include legend
				true, // tooltips
				false // urls
		);
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setRangeGridlinesVisible(true); // 是否显示格子线
		plot.setBackgroundAlpha(0.3f); // 设置背景透明度
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setAutoRangeIncludesZero(true);
		rangeAxis.setUpperMargin(0.20);
		rangeAxis.setLabelAngle(Math.PI / 2.0);
		return chart;
	}

	// 生成数据
	public static DefaultCategoryDataset createDataset() {
		DefaultCategoryDataset linedataset = new DefaultCategoryDataset();
		// 各曲线名称
		String series1 = "实际销量";
		String series2 = "预测销量";
		// String series3 = "洗衣机";
		// 横轴名称(列名称)
		// String type1 = "1月";
		// String type2 = "2月";
		// String type3 = "3月";
		// String type4 = "4月";
		for (int n = 1; n < 3000; n++) {
			linedataset.addValue(data[n][1], series1, Integer.toString(n));
		}
		for (int n = 1; n < 3651; n++) {
			linedataset.addValue(bpNetOut(pData(n))[0] * 100.0, series2, Integer.toString(n));
		}
		// linedataset.addValue(0.0, series1, type1);
		// linedataset.addValue(4.2, series1, type2);
		// linedataset.addValue(3.9, series1, type3);
		// linedataset.addValue(1.0, series2, type1);
		// linedataset.addValue(5.2, series2, type2);
		// linedataset.addValue(7.9, series2, type3);
		// linedataset.addValue(2.0, series3, type1);
		// linedataset.addValue(9.2, series3, type2);
		// linedataset.addValue(8.9, series3, type3);
		// linedataset.addValue(200, series3, type4);
		return linedataset;
	}
}
