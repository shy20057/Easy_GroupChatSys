package com.easychat.entity.query;
import com.easychat.entity.enums.PageSize;


public class SimplePage {
	private int pageNo; // 当前页码
	private int countTotal; // 总记录数
	private int pageSize; // 每页记录数
	private int pageTotal; // 总页数
	private int start; // 起始记录
	private int end; // 结束记录

	public SimplePage() {
	}

	public SimplePage(Integer pageNo, int countTotal, int pageSize) {
		if (null == pageNo) {
			pageNo = 0;
		}
		this.pageNo = pageNo;
		this.countTotal = countTotal;
		this.pageSize = pageSize;
		action();
	}

	public SimplePage(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void action() {
		if (this.pageSize <= 0) {
			this.pageSize = PageSize.SIZE20.getSize(); // 默认每页显20条记录
		}
		if (this.countTotal > 0) { // 没有余数则显示当前页数，有余数则再加一页存余数
			this.pageTotal = this.countTotal % this.pageSize == 0 ? this.countTotal / this.pageSize
					: this.countTotal / this.pageSize + 1;
		} else { //默认有一页
			pageTotal = 1;
		}

		if (pageNo <= 1) {
			pageNo = 1;
		}
		if (pageNo > pageTotal) {
			pageNo = pageTotal;
		}
		// 再用页码数 每页记录数计算起始记录和结束记录
		this.start = (pageNo - 1) * pageSize;
		this.end = this.pageSize;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getPageTotal() {
		return pageTotal;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public void setPageTotal(int pageTotal) {
		this.pageTotal = pageTotal;
	}

	public int getCountTotal() {
		return countTotal;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public void setCountTotal(int countTotal) {
		this.countTotal = countTotal;
		this.action();
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

}
