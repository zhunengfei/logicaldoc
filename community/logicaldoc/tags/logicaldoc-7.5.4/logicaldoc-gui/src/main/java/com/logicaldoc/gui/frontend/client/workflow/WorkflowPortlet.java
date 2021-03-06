package com.logicaldoc.gui.frontend.client.workflow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUIWorkflow;
import com.logicaldoc.gui.common.client.data.WorkflowTasksDS;
import com.logicaldoc.gui.common.client.formatters.DateCellFormatter;
import com.logicaldoc.gui.common.client.i18n.I18N;
import com.logicaldoc.gui.common.client.log.Log;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.common.client.util.LD;
import com.logicaldoc.gui.frontend.client.services.WorkflowService;
import com.logicaldoc.gui.frontend.client.services.WorkflowServiceAsync;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.HeaderControl.HeaderIcon;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellContextClickEvent;
import com.smartgwt.client.widgets.grid.events.CellContextClickHandler;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.ViewStateChangedEvent;
import com.smartgwt.client.widgets.grid.events.ViewStateChangedHandler;
import com.smartgwt.client.widgets.layout.Portlet;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

/**
 * Portlet specialized in listing user workflow task records.
 * 
 * @author Matteo Caruso - Logical Objects
 * @since 6.0
 */
public class WorkflowPortlet extends Portlet {

	private WorkflowServiceAsync service = (WorkflowServiceAsync) GWT.create(WorkflowService.class);

	private WorkflowTasksDS dataSource;

	private ListGrid list;

	private int type = WorkflowDashboard.TASKS_ASSIGNED;

	private WorkflowDashboard workflowDashboard;

	// We save the state of the grid to correctly handle the refreshes
	private String gridState = null;

	public WorkflowPortlet(WorkflowDashboard dashboard, int type) {
		this.workflowDashboard = dashboard;
		this.type = type;

		setCanDrag(false);
		setCanDrop(false);
		setDragAppearance(DragAppearance.OUTLINE);
		setDragOpacity(30);

		if (type == WorkflowDashboard.TASKS_ASSIGNED) {
			setTitle(I18N.message("workflowtasksassigned"));
		} else if (type == WorkflowDashboard.TASKS_I_CAN_OWN) {
			setTitle(I18N.message("workflowtaskspooled"));
		} else if (type == WorkflowDashboard.TASKS_SUSPENDED) {
			setTitle(I18N.message("workflowtaskssuspended"));
		} else if (type == WorkflowDashboard.TASKS_ADMIN) {
			setTitle(I18N.message("workflowtasksadmin"));
		} else if (type == WorkflowDashboard.TASKS_SUPERVISOR) {
			setTitle(I18N.message("workflowtaskssupervisor"));
		}

		HeaderIcon portletIcon = ItemFactory.newHeaderIcon("blank.gif");
		HeaderControl hcicon = new HeaderControl(portletIcon);
		hcicon.setSize(16);
		setHeaderControls(hcicon, HeaderControls.HEADER_LABEL);

		refresh();
	}

	public void refresh() {
		ListGridField workflow = new ListGridField("workflow", I18N.message("workflow"), 100);
		ListGridField id = new ListGridField("id", I18N.message("id"), 70);
		id.setHidden(true);
		ListGridField processId = new ListGridField("processId", I18N.message("processid"), 70);
		processId.setHidden(true);
		ListGridField name = new ListGridField("name", I18N.message("task"), 100);
		ListGridField pooledAssignees = new ListGridField("pooledassignees", I18N.message("pooledassignees"), 150);
		ListGridField documents = new ListGridField("documents", I18N.message("documents"), 300);
		ListGridField documentIds = new ListGridField("documentIds", I18N.message("documentids"), 200);
		documentIds.setHidden(true);

		ListGridField startdate = new ListGridField("startdate", I18N.message("date"), 100);
		startdate.setAlign(Alignment.CENTER);
		startdate.setType(ListGridFieldType.DATE);
		startdate.setCellFormatter(new DateCellFormatter(false));
		startdate.setCanFilter(false);

		if (list != null)
			removeItem(list);

		list = new ListGrid();
		list.setEmptyMessage(I18N.message("notitemstoshow"));
		list.setCanFreezeFields(true);
		list.setAutoFetchData(true);
		list.setShowHeader(true);
		list.setCanSelectAll(false);
		list.setSelectionType(SelectionStyle.SINGLE);
		list.setHeight100();
		list.setBorder("0px");
		dataSource = new WorkflowTasksDS(type, null);
		list.setDataSource(dataSource);
		if (type == WorkflowDashboard.TASKS_I_CAN_OWN || type == WorkflowDashboard.TASKS_ADMIN
				|| type == WorkflowDashboard.TASKS_SUPERVISOR)
			list.setFields(workflow, startdate, name, id, processId, documents, documentIds, pooledAssignees);
		else
			list.setFields(workflow, startdate, id, processId, name, documents, documentIds);

		list.addCellDoubleClickHandler(new CellDoubleClickHandler() {
			@Override
			public void onCellDoubleClick(CellDoubleClickEvent event) {
				Record record = event.getRecord();
				service.getWorkflowDetailsByTask(record.getAttributeAsString("id"), new AsyncCallback<GUIWorkflow>() {

					@Override
					public void onFailure(Throwable caught) {
						Log.serverError(caught);
					}

					@Override
					public void onSuccess(GUIWorkflow result) {
						if (result != null) {
							WorkflowDetailsDialog workflowDetailsDialog = new WorkflowDetailsDialog(workflowDashboard,
									result);
							workflowDetailsDialog.show();
						}
					}
				});
			}
		});

		list.addCellContextClickHandler(new CellContextClickHandler() {
			@Override
			public void onCellContextClick(CellContextClickEvent event) {
				showContextMenu();
				event.cancel();
			}
		});

		if (type == WorkflowDashboard.TASKS_ASSIGNED)
			// Count the total of user tasks
			list.addDataArrivedHandler(new DataArrivedHandler() {
				@Override
				public void onDataArrived(DataArrivedEvent event) {
					int total = list.getTotalRows();
					Session.get().getUser().setActiveTasks(total);
				}
			});

		/*
		 * Save the layout of the grid at every change
		 */
		list.addViewStateChangedHandler(new ViewStateChangedHandler() {
			@Override
			public void onViewStateChanged(ViewStateChangedEvent event) {
				gridState = list.getViewState();
			}
		});

		/*
		 * Restore any previously saved view state for this grid
		 */
		list.addDrawHandler(new DrawHandler() {
			@Override
			public void onDraw(DrawEvent event) {
				if (gridState != null)
					list.setViewState(gridState);
			}
		});

		addItem(list);
	}

	private void showContextMenu() {
		Menu contextMenu = new Menu();

		final ListGridRecord selection = list.getSelectedRecord();
		if (selection == null)
			return;

		/*
		 * This command will delete the Workflow instance of the currently
		 * selected task
		 */
		MenuItem delete = new MenuItem();
		delete.setTitle(I18N.message("ddelete"));
		delete.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
			public void onClick(MenuItemClickEvent event) {
				LD.ask(I18N.message("question"), I18N.message("confirmdelete"), new BooleanCallback() {
					@Override
					public void execute(Boolean value) {
						if (value) {
							// Extract the process instance ID
							String processId = selection.getAttributeAsString("processId");
							service.deleteInstance(processId, new AsyncCallback<Void>() {
								@Override
								public void onFailure(Throwable caught) {
									Log.serverError(caught);
								}

								@Override
								public void onSuccess(Void result) {
									list.deselectAllRecords();
									list.removeSelectedData();
									workflowDashboard.refresh();
								}
							});
						}
					}
				});
			}
		});

		if (!Session.get().getUser().isMemberOf("admin"))
			delete.setEnabled(false);

		contextMenu.setItems(delete);
		contextMenu.showContextMenu();
	}
}
