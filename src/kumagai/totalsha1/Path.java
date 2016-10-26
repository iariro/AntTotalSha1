package kumagai.totalsha1;

/**
 * pathタグによるパス情報。
 * @author kumagai
 */
public class Path
{
	private String path;
	private String name;

	/**
	 * パスを取得。
	 * @return パス
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * パスを割り当て。
	 * @param path パス
	 */
	public void setPath(String path)
	{
		this.path = path;
	}

	/**
	 * ターゲット名を取得。
	 * @return ターゲット名
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * ターゲット名を割り当て。
	 * @param name ターゲット名
	 */
	public void setName(String name)
	{
		this.name = name;
	}
}
