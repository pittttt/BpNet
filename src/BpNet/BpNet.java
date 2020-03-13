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

public class BpNet extends ApplicationFrame {
	private static final int IM = 1; // 输入层数量
	private static final int RM = 50; // 隐含层节点数量
	private static final int OM = 1; // 输出层数量
	private static double learnRate = 0.1; // 学习速率
	private static double alfa = 0.1; // 动量因子
	private static double Win[][] = new double[IM][RM]; // 输入到隐含连接权值
	private static double oldWin[][] = new double[IM][RM]; // 更新前上一次隐含连接权值
	private static double old1Win[][] = new double[IM][RM]; // 上上一次隐含连接权值
	private static double dWin[][] = new double[IM][RM]; // 连接权值的修正值△
	private static double Wout[][] = new double[RM][OM]; // 隐含到输出连接权值
	private static double oldWout[][] = new double[RM][OM]; // 更新前上一次隐含到输出连接权值
	private static double old1Wout[][] = new double[RM][OM]; // 上上一次隐含到输出连接权值
	private static double dWout[][] = new double[RM][OM]; // 连接权值的修正值△
	private static double Xi[] = new double[IM]; // 输入层输入
	private static double Xj[] = new double[RM]; // 隐含层输出(未经过激活函数激活)
	private static double XjActive[] = new double[RM]; // 隐含层输出(激活后)
	private static double Xk[] = new double[OM]; // 真实输出(输出层无激活函数)
	private static double Ek[] = new double[OM]; // 误差
	private static double J = 0.1; // 误差性能指标
	static int[][] data = new int[150][5]; // 存放训练数据
	static long startTime;

	public static void main(String[] arg) {
		startTime = System.currentTimeMillis(); // 或得程序开始运行时间
		// BpNet bpNet = new BpNet();

		// 训练数据导入及训练
		train();

		// 预测输出
		System.out.println("预测输出为:");
		for (int n = 46; n < 150; n++) {
			double y = data[n][1]; // 训练数据样本
			System.out.printf("%.1f  ", y); // 真实数据，用于对比误差
			System.out.printf("%f  ", bpNetOut(n / 100.0)[0] * 100.0);// 此时为单个输出，打印的值为Uk[0]
			double w = ((y - (bpNetOut(n / 100.0)[0] * 100.0)) / y) * 100;
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
		BpNet fjc = new BpNet("销量预测图");
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

	private static void train() {
		XSSFWorkbook workbook = null;
		try {
			// 读取Excel文件
			InputStream inputStream = new FileInputStream("C:\\Users\\pitt\\Desktop\\train_day.xlsx");
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
				row = sheet.getRow(i);
				if (row != null) {
					// 获取每一列的值
					// 循环列，从第二列开始
					for (int j = 1; j < row.getLastCellNum(); j++) {
						Cell cell = row.getCell(j);
						if (cell != null) {
							int value = (int) cell.getNumericCellValue();
							data[i][j] = value;
							System.out.println(data[i][j]);
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
		for (s = 0; s < 100000; s++) // 进行循环训练
		{
			for (n = 1; n < 46; n++) {

				y = data[n][1]; // 训练数据样本
				// 前向计算输出过程
				bpNetForwardProcess(n / 100.0, y / 100.0);
				// 反向学习修改权值
				bpNetReturnProcess();
			}
		}
		// 在线学习后输出
		for (n = 1; n < 46; n++) {
			y = data[n][1]; // 训练数据样本
			System.out.printf("%.1f  ", y);
			System.out.printf("%f  ", bpNetOut(n / 100.0)[0] * 100.0);// 此时为单个输出，打印的值为Uk[0]
			System.out.println("J=" + J);
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
			for (int j = 0; j < RM; j++) {
				Win[i][j] = 0.5 - Math.random();
				Xj[j] = 0;
			}
		for (int j = 0; j < RM; j++)
			for (int k = 0; k < OM; k++) {
				Wout[j][k] = 0.5 - Math.random();
				Xk[k] = 0;
			}
	}

	//
	// BP神经网络前向计算输出过程
	// @param inputParameter 归一化后的理想输入值(单个double值)
	// @param outputParameter 归一化后的理想输出值(单个double值)
	//
	public static void bpNetForwardProcess(double inputParameter, double outputParameter) {
		double input[] = { inputParameter };
		double output[] = { outputParameter };
		bpNetForwardProcess(input, output);
	}

	//
	// BP神经网络前向计算输出过程--多个输入，多个输出
	// @param inputParameter 归一化后的理想输入数组值
	// @param outputParameter 归一化后的理想输出数组值
	//
	public static void bpNetForwardProcess(double inputParameter[], double outputParameter[]) {
		for (int i = 0; i < IM; i++) {
			Xi[i] = inputParameter[i];
		}
		// 隐含层权值和计算//
		for (int j = 0; j < RM; j++) {
			Xj[j] = 0;
			for (int i = 0; i < IM; i++) {
				Xj[j] = Xj[j] + Xi[i] * Win[i][j];
			}
		}
		// 隐含层S激活输出//
		for (int j = 0; j < RM; j++) {
			XjActive[j] = 1 / (1 + Math.exp(-Xj[j]));
		}
		// 输出层权值和计算//
		for (int k = 0; k < OM; k++) {
			Xk[k] = 0;
			for (int j = 0; j < RM; j++) {
				Xk[k] = Xk[k] + XjActive[j] * Wout[j][k];
			}
		}
		// 计算输出与理想输出的偏差//
		for (int k = 0; k < OM; k++) {
			Ek[k] = outputParameter[k] - Xk[k];
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
		for (int i = 0; i < IM; i++) // 输入到隐含权值修正
		{
			for (int j = 0; j < RM; j++) {
				for (int k = 0; k < OM; k++) {
					dWin[i][j] = dWin[i][j]
							+ learnRate * (Ek[k] * Wout[j][k] * XjActive[j] * (1 - XjActive[j]) * Xi[i]);
				}
				Win[i][j] = Win[i][j] + dWin[i][j] + alfa * (oldWin[i][j] - old1Win[i][j]);
				old1Win[i][j] = oldWin[i][j];
				oldWin[i][j] = Win[i][j];
			}
		}
		for (int j = 0; j < RM; j++) // 隐含到输出权值修正
		{
			for (int k = 0; k < OM; k++) {
				dWout[j][k] = learnRate * Ek[k] * XjActive[j];
				Wout[j][k] = Wout[j][k] + dWout[j][k] + alfa * (oldWout[j][k] - old1Wout[j][k]);
				old1Wout[j][k] = oldWout[j][k];
				oldWout[j][k] = Wout[j][k];
			}
		}
	}

	//
	// BP神经网络前向计算输出，训练结束后测试输出
	// @param inputParameter 测试的归一化后的输入值
	// @return 返回归一化后的BP神经网络输出值，需逆归一化
	//
	public static double[] bpNetOut(double inputParameter) {
		double[] input = { inputParameter };
		return bpNetOut(input);
	}

	//
	// BP神经网络前向计算输出，训练结束后测试输出
	// @param inputParameter 测试的归一化后的输入数组
	// @return 返回归一化后的BP神经网络输出数组
	//
	public static double[] bpNetOut(double[] inputParameter) {
		// 在线学习后输出//
		for (int i = 0; i < IM; i++) {
			Xi[i] = inputParameter[i];
		}
		// 隐含层权值和计算//
		for (int j = 0; j < RM; j++) {
			Xj[j] = 0;
			for (int i = 0; i < IM; i++) {
				Xj[j] = Xj[j] + Xi[i] * Win[i][j];
			}
		}
		// 隐含层S激活输出//
		for (int j = 0; j < RM; j++) {
			XjActive[j] = 1 / (1 + Math.exp(-Xj[j]));
		}
		// 输出层权值和计算//
		double Uk[] = new double[OM];
		for (int k = 0; k < OM; k++) {
			Xk[k] = 0;
			for (int j = 0; j < RM; j++) {
				Xk[k] = Xk[k] + XjActive[j] * Wout[j][k];
				Uk[k] = Xk[k];
			}
		}
		return Uk;
	}

	// 图表绘制
	private static final long serialVersionUID = 1L;

	public BpNet(String s) {
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
		for (int n = 1; n < data.length; n++) {
			linedataset.addValue(data[n][1], series1, Integer.toString(n));
		}
		for (int n = 1; n < 60; n++) {
			linedataset.addValue(bpNetOut(n / 100.0)[0] * 100.0, series2, Integer.toString(n));
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
