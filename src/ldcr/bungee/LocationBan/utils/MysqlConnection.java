package ldcr.bungee.LocationBan.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ldcr.bungee.LocationBan.LocationBan;
import net.md_5.bungee.api.plugin.Plugin;

public class MysqlConnection {
	/**
	 * Create by Bkm016
	 * 
	 * Code from TabooLib
	 */

	private String url;
	private String user;
	private String port;
	private String password;
	private String database;
	private String connectionUrl;
	private Connection connection;
	private Plugin plugin;
	private final boolean fallReconnection = true;
	private int recheck = 10;
	private Thread recheckThread;

	public MysqlConnection(final String url, final String user,
			final String port, final String password, final String database) {
		this(url, user, port, password, database, 10, LocationBan.instance);
	}

	public MysqlConnection(final String url, final String user,
			final String port, final String password, final String database,
			final int recheck, final Plugin plugin) {
		// 检查驱动
		if (!loadDriverMySQL()) {
			print("错误: 无法连接到数据库, 数据库引擎驱动未安装");
			return;
		}

		// 设置信息
		this.plugin = plugin;
		this.recheck = recheck;

		// 设置数据
		this.url = url == null ? "localhost" : url;
		this.user = user == null ? "root" : user;
		this.port = port == null ? "3306" : port;
		this.password = password == null ? "" : password;
		this.database = database == null ? "test" : database;
		connectionUrl = "jdbc:mysql://" + this.url + ":" + this.port + "/"
				+ this.database + "?characterEncoding=utf-8&useSSL=false";

		// 连接数据库
		connect();

		// 断线检测
		recheckThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(getReCheckSeconds() * 1000);
						if (connection == null) {
							print("警告! 数据库尚未连接, 请检查配置文件后重启服务器!");
							continue;
						} else {
							isExists("LdcrUtils");
						}
					} catch (final Exception e) {
						new RuntimeException("数据库命令执行出错",e).printStackTrace();
					}

				}
			}
		});

		// 启动检测
		if (isConnection()) {
			recheckThread.setDaemon(true);
			recheckThread.start();
			print("正在启动数据库监视线程...");
		}
	}

	public void setReCheckSeconds(final int s) {
		recheck = s;
	}

	public int getReCheckSeconds() {
		return recheck;
	}

	public boolean isConnection() {
		try {
			if ((connection == null) || connection.isClosed())
				return false;
		} catch (final SQLException e) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void closeConnection() {
		try {
			connection.close();
		} catch (final Exception e) {
			//
		}
		try {
			recheckThread.stop();
		} catch (final Exception e) {
			//
		}
	}

	public boolean deleteTable(final String name) {
		return execute("drop table if exists " + name);
	}

	/**
	 * 2018年1月17日 新增, TabooLib 版本 3.25
	 */
	public boolean truncateTable(final String name) {
		return execute("truncate table " + name);
	}

	public boolean clearTable(final String name) {
		return execute("delete from " + name);
	}

	public boolean renameTable(final String name, final String newName) {
		return execute("rename table `" + name + "` to `" + newName + "`");
	}

	public boolean deleteColumn(final String name, final String column) {
		return execute("alter table `" + name + "` drop `" + column + "`");
	}

	public void addColumn(final String name, final Column... columns) {
		for (final Column column : columns) {
			execute("alter table " + name + " add " + column.toString());
		}
	}

	public boolean addColumn(final String name, final String column) {
		if (!column.contains("/"))
			return execute("alter table " + name + " add `" + column + "` text");
		return execute("alter table " + name + " add `" + column.split("/")[0]
				+ "` " + column.split("/")[1]);
	}

	public boolean editColumn(final String name, final String oldColumn,
			final Column newColumn) {
		return execute("alter table " + name + " change `" + oldColumn + "` "
				+ newColumn.toString());
	}

	public boolean editColumn(final String name, final String oldColumn,
			final String newColumn) {
		if (!newColumn.contains("/"))
			return execute("alter table " + name + " change `" + oldColumn
			               + "` `" + newColumn + "` text");
		return execute("alter table " + name + " change `" + oldColumn + "` `"
				+ newColumn.split("/")[0] + "` " + newColumn.split("/")[1]);
	}

	/**
	 * 删除数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @return boolean
	 */
	public boolean deleteValue(final String name, final String column,
			final Object columnValue) {
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("delete from `" + name
			                                    + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 写入数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @param value
	 *            数据值
	 * @return boolean
	 */
	public boolean setValue(final String name, final String column,
			final Object columnValue, final String valueColumn,
			final Object value) {
		return setValue(name, column, columnValue, valueColumn, value, false);
	}

	/**
	 * 写入数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @param value
	 *            数据值
	 * @param append
	 *            是否追加（数据列类型必须为数字）
	 * @return boolean
	 */
	public boolean setValue(final String name, final String column,
			final Object columnValue, final String valueColumn,
			final Object value, final boolean append) {
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			if (append) {
				pstmt = connection.prepareStatement("update `" + name
				                                    + "` set `" + valueColumn + "` = `" + valueColumn
				                                    + "` + ? where `" + column + "` = ?");
			} else {
				pstmt = connection.prepareStatement("update `" + name
				                                    + "` set `" + valueColumn + "` = ? where `" + column
				                                    + "` = ?");
			}
			pstmt.setObject(1, value);
			pstmt.setObject(2, columnValue);
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 插入数据
	 * 
	 * @param name
	 *            名称
	 * @param values
	 *            值
	 * @return boolean
	 */
	public boolean intoValue(final String name, final Object... values) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append("?, ");
		}
		PreparedStatement pstmt = null;
		final ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("insert into `" + name
			                                    + "` values(null, " + sb.substring(0, sb.length() - 2)
			                                    + ")");
			for (int i = 0; i < values.length; i++) {
				pstmt.setObject(i + 1, values[i]);
			}
			pstmt.executeUpdate();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 创建数据表
	 * 
	 * @param name
	 *            名称
	 * @param columns
	 *            列表
	 * @return boolean
	 */
	public boolean createTable(final String name, final Column... columns) {
		final StringBuilder sb = new StringBuilder();
		for (final Column column : columns) {
			sb.append(column.toString() + ", ");
		}
		return execute("create table if not exists " + name
		               + " (id int(1) not null primary key auto_increment, "
		               + sb.substring(0, sb.length() - 2) + ")");
	}

	/**
	 * 创建数据表
	 * 
	 * @param name
	 *            名称
	 * @param columns
	 *            列表
	 * @return boolean
	 */
	public boolean createTable(final String name, final String... columns) {
		final StringBuilder sb = new StringBuilder();
		for (final String column : columns) {
			if (!column.contains("/")) {
				sb.append("`" + column + "` text, ");
			} else {
				sb.append("`" + column.split("/")[0] + "` "
						+ column.split("/")[1] + ", ");
			}
		}
		return execute("create table if not exists " + name
		               + " (id int(1) not null primary key auto_increment, "
		               + sb.substring(0, sb.length() - 2) + ")");
	}

	/**
	 * 检查数据表是否存在
	 * 
	 * @param name
	 *            名称
	 * @return boolean
	 */
	public boolean isExists(final String name) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection
					.prepareStatement("select table_name FROM information_schema.TABLES where table_name = ?");
			pstmt.setString(1, name);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 检查数据是否存在
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            列表名
	 * @param columnValue
	 *            列表值
	 * @return boolean
	 */
	public boolean isExists(final String name, final String column,
			final Object columnValue) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name
			                                    + "` where `" + column + "` = ?");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return false;
	}

	/**
	 * 获取所有列表名称（不含主键）
	 * 
	 * @param name
	 *            名称
	 * @return {@link List}
	 */
	public List<String> getColumns(final String name) {
		return getColumns(name, false);
	}

	/**
	 * 获取所有列表名称
	 * 
	 * @param name
	 *            名称
	 * @param primary
	 *            是否获取主键
	 * @return {@link List}
	 */
	public List<String> getColumns(final String name, final boolean primary) {
		final List<String> list = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection
					.prepareStatement("select column_name from information_schema.COLUMNS where table_name = ?");
			pstmt.setString(1, name);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getString(1));
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		// 是否获取主键
		if (!primary) {
			list.remove("id");
		}
		return list;
	}

	/**
	 * 获取单项数据
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return Object
	 */
	public Object getValue(final String name, final String column,
			final Object columnValue, final String valueColumn) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name
			                                    + "` where `" + column + "` = ? limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return null;
	}

	/**
	 * 获取单项数据（根据主键倒叙排列后的最后一项）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return Object
	 */
	public Object getValueLast(final String name, final String column,
			final Object columnValue, final String valueColumn) {
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name
			                                    + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next())
				return resultSet.getObject(valueColumn);
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return null;
	}

	/**
	 * 获取多项数据（根据主键倒叙排列后的最后一项）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return {@link HashMap}
	 */
	public HashMap<String, Object> getValueLast(final String name,
			final String column, final Object columnValue,
			final String... valueColumn) {
		final HashMap<String, Object> map = new HashMap<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name
			                                    + "` where `" + column + "` = ? order by id desc limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				break;
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return map;
	}

	/**
	 * 获取多项数据（单项多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param columnValue
	 *            参考值
	 * @param valueColumn
	 *            数据列
	 * @return {@link HashMap}
	 */
	public HashMap<String, Object> getValue(final String name,
			final String column, final Object columnValue,
			final String... valueColumn) {
		final HashMap<String, Object> map = new HashMap<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name
			                                    + "` where `" + column + "` = ? limit 1");
			pstmt.setObject(1, columnValue);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				break;
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return map;
	}

	/**
	 * 获取多项数据（单列多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @return {@link List}
	 */
	public List<Object> getValues(final String name, final String column,
			final int size) {
		return getValues(name, column, size, false);
	}

	/**
	 * 获取多项数据（单列多列）
	 * 
	 * @param name
	 *            名称
	 * @param column
	 *            参考列
	 * @param size
	 *            获取数量（-1 位无限制）
	 * @param desc
	 *            是否倒序
	 * @return {@link List}
	 */
	public List<Object> getValues(final String name, final String column,
			final int size, final boolean desc) {
		final List<Object> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement("select * from `" + name
				                                    + "` order by ? desc "
				                                    + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement("select * from `" + name
				                                    + "` order by ? " + (size < 0 ? "" : " limit " + size));
			}
			pstmt.setString(1, column);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				list.add(resultSet.getObject(column));
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	/**
	 * 获取多线数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name,
			final String sortColumn, final int size,
			final String... valueColumn) {
		return getValues(name, sortColumn, size, false, valueColumn);
	}

	/**
	 * 获取多项数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param desc
	 *            是否倒序
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name,
			final String sortColumn, final int size, final boolean desc,
			final String... valueColumn) {
		final LinkedList<HashMap<String, Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			if (desc) {
				pstmt = connection.prepareStatement("select * from `" + name
				                                    + "` order by ? desc "
				                                    + (size < 0 ? "" : " limit " + size));
			} else {
				pstmt = connection.prepareStatement("select * from `" + name
				                                    + "` order by ? " + (size < 0 ? "" : " limit " + size));
			}
			pstmt.setString(1, sortColumn);
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final HashMap<String, Object> map = new HashMap<>();
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				list.add(map);
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	/**
	 * 获取多项数据（多项多列）
	 * 
	 * @param name
	 *            名称
	 * @param sortColumn
	 *            参考列（该列类型必须为数字）
	 * @param size
	 *            获取数量（-1 为无限制）
	 * @param desc
	 *            是否倒序
	 * @param valueColumn
	 *            获取数据列
	 * @return {@link LinkedList}
	 */
	public LinkedList<HashMap<String, Object>> getValues(final String name,
			final int size, final String... valueColumn) {
		final LinkedList<HashMap<String, Object>> list = new LinkedList<>();
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = connection.prepareStatement("select * from `" + name + "` "
					+ (size < 0 ? "" : " limit " + size));
			resultSet = pstmt.executeQuery();
			while (resultSet.next()) {
				final HashMap<String, Object> map = new HashMap<>();
				for (final String _column : valueColumn) {
					map.put(_column, resultSet.getObject(_column));
				}
				list.add(map);
			}
		} catch (final Exception e) {
			new RuntimeException("数据库命令执行出错",e).printStackTrace();
			// 重新连接
			if (fallReconnection && e.getMessage().contains("closed")) {
				connect();
			}
		} finally {
			freeResult(resultSet, pstmt);
		}
		return list;
	}

	public boolean execute(final String sql) {
		PreparedStatement pstmt = null;
		try {
			pstmt = connection.prepareStatement(sql);
			pstmt.execute();
			return true;
		} catch (final Exception e) {
			new RuntimeException("数据库命令 {" +sql+"} 执行出错",e).printStackTrace();
			// 重连
			if (e.getMessage().contains("closed")) {
				connect();
			}
			return false;
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (final Exception e) {
				//
			}
		}
	}

	public boolean connect() {
		try {
			print("正在连接数据库");
			print("地址: " + connectionUrl);
			final long time = System.currentTimeMillis();
			connection = DriverManager.getConnection(connectionUrl, user,
			                                         password);
			print("数据库连接成功 (" + (System.currentTimeMillis() - time) + "ms)");
			return true;
		} catch (final SQLException e) {
			print("数据库连接失败");
			print("错误原因: " + e.getMessage());
			print("错误代码: " + e.getErrorCode());
			return false;
		}
	}

	public void print(final String message) {
		plugin.getLogger().info("Mysql > " + message);
	}

	/**
	 * 释放结果集
	 * 
	 * @param resultSet
	 *            不知道叫什么
	 * @param pstmt
	 *            不知道叫什么
	 */
	private void freeResult(final ResultSet resultSet,
			final PreparedStatement pstmt) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (final Exception e) {
			//
		}
		try {
			if (pstmt != null) {
				pstmt.close();
			}
		} catch (final Exception e) {
			//
		}
	}

	private boolean loadDriverMySQL() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	public static enum ColumnInteger {
		TINYINT, SMALLINT, MEDIUMINT, INT, BIGINT;
	}

	public static enum ColumnFloat {
		FLOAT, DOUBLE;
	}

	public static enum ColumnChar {
		CHAR, VARCHAR;
	}

	public static enum ColumnString {
		TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT;
	}

	public static class Column {

		private final String name;
		private Object type;
		private int a;
		private int b;

		public Column(final String name) {
			this.name = name;
			type = ColumnString.TEXT;
		}

		public Column(final String name, final ColumnInteger type) {
			this(name);
			this.type = type;
			a = 12;
		}

		public Column(final String name, final ColumnInteger type, final int m) {
			this(name);
			this.type = type;
			a = m;
		}

		public Column(final String name, final ColumnFloat type, final int m,
				final int d) {
			this(name);
			this.type = type;
			a = m;
			b = d;
		}

		public Column(final String name, final ColumnChar type, final int n) {
			this(name);
			this.type = type;
			a = n;
		}

		public Column(final String name, final ColumnString type) {
			this(name);
			this.type = type;
		}

		@Override
		public String toString() {
			if ((type instanceof ColumnInteger) || (type instanceof ColumnChar))
				return "`" + name + "` " + type.toString().toLowerCase() + "("
				+ a + ")";
			else if (type instanceof ColumnFloat)
				return "`" + name + "` " + type.toString().toLowerCase() + "("
				+ a + "," + b + ")";
			else
				return "`" + name + "` " + type.toString().toLowerCase();
		}
	}
}
