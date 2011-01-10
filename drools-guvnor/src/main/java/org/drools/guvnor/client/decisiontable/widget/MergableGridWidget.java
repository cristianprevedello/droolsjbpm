package org.drools.guvnor.client.decisiontable.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.drools.guvnor.client.decisiontable.widget.DecisionTableWidget.MOVE_DIRECTION;
import org.drools.guvnor.client.resources.DecisionTableResources;
import org.drools.guvnor.client.resources.DecisionTableResources.DecisionTableStyle;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

/**
 * A minimal CellTable replacement that renders merged cells and handles basic
 * events. No keyboard navigation implemented.
 * 
 * @author manstis
 * 
 */
public abstract class MergableGridWidget extends Widget {

	// Data to render
	protected List<DynamicColumn> columns = new ArrayList<DynamicColumn>();
	protected List<DynamicDataRow> data = new ArrayList<DynamicDataRow>();

	// TABLE elements
	protected TableElement table;
	protected TableSectionElement tbody;

	// Resources
	protected static final DecisionTableResources resource = GWT
			.create(DecisionTableResources.class);

	protected static final DecisionTableStyle style = resource.cellTableStyle();

	// The DecisionTable to which this grid belongs. This is used solely
	// to record when a cell has been clicked as the DecisionTable manages
	// writing values back to merged cells...
	protected DecisionTableWidget dtable;

	protected DecisionTableHeaderWidget headerWidget;

	protected DecisionTableSidebarWidget sideBarWidget;

	/**
	 * A grid of possibly merged cells.
	 * 
	 * @param dtable
	 *            DecisionTable to which the grid is a child
	 * @param resource
	 *            ClientBundle for the grid
	 */
	public MergableGridWidget(DecisionTableWidget dtable) {

		this.dtable = dtable;
		this.sideBarWidget = dtable.getSidebarWidget();
		this.headerWidget = dtable.getHeaderWidget();

		style.ensureInjected();

		// Create some elements to contain the grid
		table = Document.get().createTableElement();
		tbody = Document.get().createTBodyElement();
		table.setClassName(style.cellTable());
		table.setCellPadding(0);
		table.setCellSpacing(0);
		setElement(table);

		table.appendChild(tbody);

		// Events in which we're interested (note, if a Cell<?> appears not to
		// work I've probably forgotten some events. Might be a better way of
		// doing this, but I copied CellTable<?, ?>'s lead
		sinkEvents(Event.getTypeInt("click") | Event.getTypeInt("dblclick")
				| Event.getTypeInt("mouseover") | Event.getTypeInt("mouseout")
				| Event.getTypeInt("change") | Event.getTypeInt("keypress")
				| Event.getTypeInt("keydown"));
	}

	/**
	 * Retrieve the extents of a cell
	 * 
	 * @param cv
	 *            The cell for which to retrieve the extents
	 * @return
	 */
	public CellExtents getSelectedCellExtents(
			CellValue<? extends Comparable<?>> cv) {

		if (cv == null) {
			throw new IllegalArgumentException("CellValue cannot be null");
		}

		// Cells in hidden columns do not have extents
		if (!columns.get(cv.getCoordinate().getCol()).getIsVisible()) {
			return null;
		}

		Coordinate hc = cv.getHtmlCoordinate();
		TableRowElement tre = tbody.getRows().getItem(hc.getRow())
				.<TableRowElement> cast();
		TableCellElement tce = tre.getCells().getItem(hc.getCol())
				.<TableCellElement> cast();
		int x = tce.getOffsetLeft();
		int y = tce.getOffsetTop();
		int w = tce.getOffsetWidth();
		int h = tce.getOffsetHeight();
		CellExtents e = new CellExtents(x, y, h, w);
		return e;
	}

	/**
	 * Container for a cell's extents
	 * 
	 * @author manstis
	 * 
	 */
	public static class CellExtents {
		private int x;
		private int y;
		private int height;
		private int width;

		CellExtents(int x, int y, int height, int width) {
			this.x = x;
			this.y = y;
			this.height = height;
			this.width = width;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getHeight() {
			return height;
		}

		public int getWidth() {
			return width;
		}

	}

	/**
	 * Add a column at the end of the list of columns
	 * 
	 * @param column
	 */
	public void addColumn(DynamicColumn column) {
		insertColumnBefore(columns.size(), column);
	}

	/**
	 * Delete the row at the given index. Partial redraw.
	 * 
	 * @param index
	 */
	public abstract void deleteRow(int index);

	/**
	 * Remove styling indicating a selected state
	 * 
	 * @param cell
	 */
	public abstract void deselectCell(CellValue<? extends Comparable<?>> cell);

	/**
	 * Get a list of columns (Woot, CellTable lacks this!)
	 * 
	 * @return
	 */
	public List<DynamicColumn> getColumns() {
		return this.columns;
	}

	/**
	 * Hide a column
	 */
	public abstract void hideColumn(int index);

	/**
	 * Add a column at a specific index
	 * 
	 * @param index
	 * @param column
	 */
	public void insertColumnBefore(int index, DynamicColumn column) {
		columns.add(index, column);

		// Re-index columns
		for (int iCol = 0; iCol < columns.size(); iCol++) {
			DynamicColumn col = columns.get(iCol);
			col.setColumnIndex(iCol);
		}

		headerWidget.insertColumnBefore(index, column);
	}

	/**
	 * Insert the given row before the provided index. Partial redraw.
	 * 
	 * @param index
	 *            The index of the row before which the new row should be
	 *            inserted
	 * @param rowData
	 *            The row of data to insert
	 */
	public abstract void insertRowBefore(int index, DynamicDataRow rowData);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.gwt.user.client.ui.Widget#onBrowserEvent(com.google.gwt.user
	 * .client.Event)
	 */
	@Override
	public void onBrowserEvent(Event event) {

		String eventType = event.getType();

		// Get the event target.
		EventTarget eventTarget = event.getEventTarget();
		if (!Element.is(eventTarget)) {
			return;
		}
		Element target = event.getEventTarget().cast();

		// Find the cell where the event occurred.
		TableCellElement tableCell = findNearestParentCell(target);
		if (tableCell == null) {
			return;
		}
		int htmlCol = tableCell.getCellIndex();

		Element trElem = tableCell.getParentElement();
		if (trElem == null) {
			return;
		}
		TableRowElement tr = TableRowElement.as(trElem);
		int htmlRow = tr.getSectionRowIndex();

		// Convert HTML coordinates to physical coordinates
		Element parent = getCellParent(tableCell);
		DynamicDataRow htmlRowData = data.get(htmlRow);
		CellValue<? extends Comparable<?>> htmlCell = htmlRowData.get(htmlCol);
		Coordinate c = htmlCell.getPhysicalCoordinate();
		CellValue<? extends Comparable<?>> physicalCell = data.get(c.getRow())
				.get(c.getCol());

		// Select range
		if (eventType.equals("click")) {
			dtable.startSelecting(c);
		}

		// Keyboard navigation
		if (eventType.equals("keypress")) {
			if (event.getKeyCode() == KeyCodes.KEY_DELETE) {
				dtable.update(null);
			} else if (event.getKeyCode() == KeyCodes.KEY_RIGHT
					|| (event.getKeyCode() == KeyCodes.KEY_TAB && !event
							.getShiftKey())) {
				dtable.moveSelection(MOVE_DIRECTION.RIGHT);
				event.preventDefault();
			} else if (event.getKeyCode() == KeyCodes.KEY_LEFT
					|| (event.getKeyCode() == KeyCodes.KEY_TAB && event
							.getShiftKey())) {
				dtable.moveSelection(MOVE_DIRECTION.LEFT);
				event.preventDefault();
			} else if (event.getKeyCode() == KeyCodes.KEY_UP) {
				dtable.moveSelection(MOVE_DIRECTION.UP);
				event.preventDefault();
			} else if (event.getKeyCode() == KeyCodes.KEY_DOWN) {
				dtable.moveSelection(MOVE_DIRECTION.DOWN);
				event.preventDefault();
			}
		}

		// Enter key is a special case; as the selected cell needs to be sent
		// events and not the cell that GWT deemed the target for events.
		if (eventType.equals("keydown") || eventType.equals("keypress")
				|| eventType.equals("keyup")) {
			if (event.getKeyCode() == KeyCodes.KEY_ENTER) {

				physicalCell = dtable.getSelections().first();
				c = physicalCell.getCoordinate();
				parent = getCellParent(tbody.getRows()
						.getItem(physicalCell.getHtmlCoordinate().getRow())
						.getCells()
						.getItem(physicalCell.getHtmlCoordinate().getCol()));

			}
		}

		// Pass event and physical cell to Cell Widget for handling
		Cell<CellValue<? extends Comparable<?>>> cellWidget = columns.get(
				c.getCol()).getCell();

		// Implementations of AbstractCell aren't forced to initialise
		// consumed events
		Set<String> consumedEvents = cellWidget.getConsumedEvents();
		if (consumedEvents != null && consumedEvents.contains(eventType)) {
			cellWidget.onBrowserEvent(parent, physicalCell, null, event, null);
		}
	}

	/**
	 * Redraw the whole table
	 */
	public abstract void redraw();

	/**
	 * Redraw table column. Partial redraw
	 * 
	 * @param index
	 *            Start column index (inclusive)
	 */
	public abstract void redrawColumn(int index);

	/**
	 * Redraw table columns from index to the end. Partial redraw
	 * 
	 * @param startRedrawIndex
	 *            Start column index (inclusive)
	 * @param endRedrawIndex
	 *            End column index (inclusive)
	 */
	public abstract void redrawColumns(int startRedrawIndex, int endRedrawIndex);

	/**
	 * Redraw a section of the table. Partial redraw
	 * 
	 * @param startRedrawIndex
	 *            Start row index (inclusive)
	 * @param endRedrawIndex
	 *            End row index (inclusive)
	 */
	public abstract void redrawRows(int startRedrawIndex, int endRedrawIndex);

	/**
	 * Delete all columns
	 */
	public void removeAllColumns() {
		columns.clear();
	}

	/**
	 * Remove a column at a specific index
	 * 
	 * @param index
	 */
	public void removeColumn(int index) {
		columns.remove(index);
	}

	/**
	 * Add styling to cell to indicate a selected state
	 * 
	 * @param cell
	 */
	public abstract void selectCell(CellValue<? extends Comparable<?>> cell);

	public void setColumnVisibility(int index, boolean isVisible) {
		this.columns.get(index).setIsVisible(isVisible);
	}

	/**
	 * Set the data to be rendered.
	 * 
	 * @param data
	 */
	public void setRowData(List<DynamicDataRow> data) {
		this.data = data;
	}

	/**
	 * Show a column
	 */
	public abstract void showColumn(int index);

	// Find the cell that contains the element. Note that the TD element is not
	// the parent. The parent is the div inside the TD cell.
	private TableCellElement findNearestParentCell(Element elem) {
		while ((elem != null) && (elem != table)) {
			String tagName = elem.getTagName();
			if ("td".equalsIgnoreCase(tagName)
					|| "th".equalsIgnoreCase(tagName)) {
				return elem.cast();
			}
			elem = elem.getParentElement();
		}
		return null;
	}

	// Get the parent element that is passed to the {@link Cell} from the table
	// cell element.
	private Element getCellParent(TableCellElement td) {
		return td.getFirstChildElement();
	}

}
