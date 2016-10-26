package kumagai.totalsha1;

import java.io.*;
import java.security.*;
import java.util.*;
import org.apache.tools.ant.*;
import ktool.datetime.*;

/**
 * プロジェクト全ファイルSHA1ハッシュ取得タスク。
 * @author kumagai
 */
public class TotalSha1Task
	extends Task
{
	private String file;
	private String excludefolders;
	private String excludeextensions;
	private String outtype;

	private ArrayList<Path> pathCollection = new ArrayList<Path>();

	/**
	 * ファイルパスを取得。
	 * @return ファイルパス
	 */
	public String getFile()
	{
		return file;
	}

	/**
	 * 除外フォルダパスを取得。区切りはスペース。
	 * @return フォルダパス
	 */
	public String getExcludefolders()
	{
		return excludefolders;
	}

	/**
	 * 除外フォルダパスを割り当て。区切りはスペース。
	 * @param excludefolders フォルダパス
	 */
	public void setExcludefolders(String excludefolders)
	{
		this.excludefolders = excludefolders;
	}

	/**
	 * 除外拡張子を取得。
	 * @return 除外拡張子
	 */
	public String getExcludeextensions()
	{
		return excludeextensions;
	}

	/**
	 * 除外拡張子を割り当てる。
	 * @param excludeextensions 除外拡張子
	 */
	public void setExcludeextensions(String excludeextensions)
	{
		this.excludeextensions = excludeextensions;
	}

	/**
	 * ファイルパスを割り当てる。
	 * @param file ファイルパス
	 */
	public void setFile(String file)
	{
		this.file = file;
	}

	/**
	 * パス情報生成。
	 * @return パス情報
	 */
	public Path createPath()
	{
		Path path = new Path();

		pathCollection.add(path);

		return path;
	}

	/**
	 * 出力タイプを取得。
	 * @return 出力タイプ
	 */
	public String getOuttype()
	{
		return outtype;
	}

	/**
	 * 出力タイプを割り当て。
	 * @param outtype 出力タイプ
	 */
	public void setOuttype(String outtype)
	{
		this.outtype = outtype;
	}

	/**
	 * 実行。
	 */
	public void execute()
		throws BuildException
	{
		boolean fileOut;
		PrintStream out;

		if (file != null)
		{
			// ファイル指定あり。

			try
			{
				out = new PrintStream(file);
				fileOut = true;
			}
			catch (IOException exception)
			{
				System.out.println(exception);
				return;
			}
		}
		else
		{
			// ファイル指定なし。

			out = System.out;
			fileOut = false;
		}

		for (Path path : pathCollection)
		{
			if (new File(path.getPath()).list() != null)
			{
				// パスあり。

				try
				{
					MessageDigest messageDigest =
						MessageDigest.getInstance("SHA1");

					DirectoryStatistics statistics =
						executeRecursive(path.getPath(), messageDigest, out);

					byte [] messageDigestByteArray = messageDigest.digest();

					for (int i=0 ; i<messageDigestByteArray.length ; i++)
					{
						out.printf("%02X", messageDigestByteArray[i]);
					}

					DateTime lastModified =
						new DateTime(statistics.lastModified);
					String lastModified2 = lastModified.toFullString();

					out.printf(
						" (%3d)(%s) : %s",
						statistics.count,
						lastModified2.substring(2, 16),
						path.getName());

					if (! statistics.packageInfo)
					{
						// package-info.javaファイルなし。

						out.print("*");
					}

					out.println();
				}
				catch (Exception exception)
				{
					exception.printStackTrace();
				}
			}
			else
			{
				// パスなし。

				out.println(path.getPath() + " not found");
			}
		}

		if (fileOut)
		{
			// ファイル出力の場合。

			System.out.printf("%s out\n", file);
			out.close();
		}

		super.execute();
	}

	/**
	 * 実処理。
	 * @param dir ディレクトリ
	 * @param messageDigest ハッシュ値計算オブジェクト
	 * @param out 出力ストリーム
	 * @return ディレクトリ集計情報
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public DirectoryStatistics executeRecursive
		(String dir, MessageDigest messageDigest, PrintStream out)
		throws IOException, NoSuchAlgorithmException
	{
		DirectoryStatistics statistics = new DirectoryStatistics();

		int javaCount = 0;

		for (File file : new File(dir).listFiles())
		{
			if (! file.isDirectory())
			{
				// ファイル。

				if (!file.getName().equals("package-info.java") &&
					file.getName().endsWith(".java"))
				{
					// .javaファイルである。

					javaCount++;
				}
			}
		}

		if (javaCount <= 0 ||
			(dir.indexOf("src\\") < 0) ||
			(dir.indexOf("junit") >= 0) ||
			(dir.indexOf("test") >= 0))
		{
			// srcフォルダ以外、テストコードフォルダ、srcフォルダ以下でもjava
			// ファイルがないフォルダの場合。

			statistics.packageInfo = true;
		}

		// ファイル→ディレクトリの順でチェックするため一旦コレクションを作成。
		ArrayList<File> files = new ArrayList<File>();
		ArrayList<File> dirs = new ArrayList<File>();

		for (File file : new File(dir).listFiles())
		{
			if (file.isDirectory())
			{
				// ディレクトリ。

				dirs.add(file);
			}
			else
			{
				// ファイル。

				files.add(file);
			}
		}

		for (File file : files)
		{
			// ファイル。

			if (file.getName().equals("package-info.java") && javaCount >= 1)
			{
				// package-info.javaファイルであり、それ以外のjavaファイルがある。

				statistics.packageInfo = true;
			}

			boolean find = false;

			if (excludeextensions != null)
			{
				// 指定あり。

				for (String exclude : excludeextensions.split(" "))
				{
					if (file.getName().endsWith(exclude))
					{
						// 一致する。

						find = true;
						break;
					}
				}
			}

			if (! find)
			{
				// 除外ファイルではない。

				FileInputStream input = new FileInputStream(file.getPath());
				byte [] buffer = new byte [(int)file.length()];
				input.read(buffer, 0, (int)file.length());
				input.close();

				messageDigest.update(buffer);

				statistics.count++;

				if (statistics.lastModified < file.lastModified())
				{
					// 最新の更新日時。

					statistics.lastModified = file.lastModified();
				}

				if (outtype != null)
				{
					// 出力タイプ指定あり。

					MessageDigest messageDigest2 =
						MessageDigest.getInstance("SHA1");

					messageDigest2.update(buffer);

					byte [] messageDigestByteArray =
						messageDigest2.digest();

					out.print("\t");

					for (int i=0 ; i<messageDigestByteArray.length ; i++)
					{
						out.printf("%02X", messageDigestByteArray[i]);
					}

					out.println(" " + file.getName());
				}
			}
		}

		for (File file : dirs)
		{
			boolean find = false;

			if (excludefolders != null)
			{
				// 指定あり。

				for (String exclude : excludefolders.split(" "))
				{
					if (file.getName().equals(exclude))
					{
						// 一致する。

						find = true;
						break;
					}
				}
			}

			if (! find)
			{
				// 除外フォルダではない。

				DirectoryStatistics statistics2 =
					executeRecursive(file.getPath(), messageDigest, out);
				statistics.count += statistics2.count;

				if (statistics.lastModified < statistics2.lastModified)
				{
					// 最新の更新日時。

					statistics.lastModified = statistics2.lastModified;
				}

				statistics.packageInfo &= statistics2.packageInfo;
			}
		}

		if (! statistics.packageInfo)
		{
			out.printf("%s %d\n", dir, javaCount);
		}

		return statistics;
	}
}
