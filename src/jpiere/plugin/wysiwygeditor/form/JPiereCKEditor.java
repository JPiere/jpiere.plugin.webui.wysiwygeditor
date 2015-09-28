/******************************************************************************
 * Product: JPiere(Japan + iDempiere)                                         *
 * Copyright (C) Hideaki Hagiwara (h.hagiwara@oss-erp.co.jp)                  *
 *                                                                            *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY.                          *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 *  JPiere is maintained by OSS ERP Solutions Co., Ltd.                       *
 * (http://www.oss-erp.co.jp)                                                 *
 *****************************************************************************/
package jpiere.plugin.wysiwygeditor.form;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;



import org.adempiere.base.IModelFactory;
import org.adempiere.base.Service;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditorPopupMenu;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ContextMenuListener;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.model.GridTab;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MLanguage;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MToolBarButton;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.zkforge.ckez.CKeditor;
import org.zkoss.zhtml.Text;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.impl.XulElement;

/**
 * JPiere CKEditor
 *
 * JPIERE-0109
 *
 * @author Hideaki Hagiwara(h.hagiwara@oss-erp.co.jp)
 *
 */
public class JPiereCKEditor implements EventListener<Event>, ValueChangeListener, IFormController{

	/**	Logger			*/
	private  static CLogger log = CLogger.getCLogger(JPiereCKEditor.class);
	
	private String baselang = Language.getBaseAD_Language();

	private JPiereCKEditorForm   wysiwygEditorForm =null;

	private String baseTableName ;
	private String trlTableName ;
	
	private String columnName ;
	private String keyColumnName;
	
	private int record_ID = 0;
	
	private PO po ;
	private boolean isMultiLingual = true;
	private boolean isSameClientData = false;
	
	protected Combobox lstLanguage;

	/**********************************************************************
	 * UI Component
	 **********************************************************************/

	private Borderlayout mainLayout = new Borderlayout();
	private North north = new North();
	private Center center = new Center();
	private South south = new South();

	private Panel parameterPanel = new Panel();	
	private Grid parameterLayout = GridFactory.newGridLayout();

	private WTableDirEditor tableDirEditor;
	
	private Button SaveButton;

	private CKeditor ckeditor = new CKeditor();


	/**
	 * Constractor
	 *
	 * @throws IOException
	 */
    public JPiereCKEditor() throws IOException
    {
    	wysiwygEditorForm = new JPiereCKEditorForm(this);
		LayoutUtils.addSclass("jpiere-ckeditor-form", wysiwygEditorForm);
    }

	public ADForm getForm()
	{
 
		return wysiwygEditorForm;
	}


	public void initForm() 
	{	
		String errorMessage = prepare();
		if(errorMessage != null)
		{
			north.appendChild(new Text(errorMessage));
			mainLayout.appendChild(north);
			return;
		}
		zkInit();
		LayoutUtils.sendDeferLayoutEvent(mainLayout, 100);
	}
	
	private String prepare() 
	{
		wysiwygEditorForm.setSizable(true);
		wysiwygEditorForm.setClosable(true);
		wysiwygEditorForm.setMaximizable(true);
		wysiwygEditorForm.setWidth("95%");
		wysiwygEditorForm.setHeight("80%");
		wysiwygEditorForm.appendChild (mainLayout);
		LayoutUtils.addSclass("jpiere-ckeditor-form-content", mainLayout);
		wysiwygEditorForm.setBorder("normal");
		
		ProcessInfo pInfo = wysiwygEditorForm.getProcessInfo();
		
		if(pInfo == null)
		{
			return "ProcessInfo is Null";
		}else{
			record_ID = pInfo.getRecord_ID();
		}
		
		
		GridTab gTab = wysiwygEditorForm.getGridTab();
		baseTableName = gTab.getTableName();
		
		MTable table_Trl = MTable.get(Env.getCtx(), baseTableName + "_Trl");
		MClient client = MClient.get(Env.getCtx());

		if(table_Trl == null)
		{
			isMultiLingual = false;
		
		}else if(!client.isMultiLingualDocument()){
			
			isMultiLingual = false;
			
		}else{
			
			isMultiLingual = true;
			trlTableName = baseTableName + "_Trl";
		}
			
		MToolBarButton[]  toolBarButtons =MToolBarButton.getProcessButtonOfTab(gTab.getAD_Tab_ID(),null);
		int counter = 0;
		for(int i = 0; toolBarButtons.length > i; i++)
		{
			
			if(toolBarButtons[i].getAD_Process_ID() == pInfo.getAD_Process_ID())
			{
				counter++;
				if(counter > 1)
				{
					return pInfo.getTitle()+" process can set only one per one tab";
				}
				
				columnName = toolBarButtons[i].getComponentName();
				
			}
		}
		
		
		List<IModelFactory> factoryList = Service.locator().list(IModelFactory.class).getServices();
		if (factoryList != null)
		{
			for(IModelFactory factory : factoryList) {
				po = factory.getPO(baseTableName, record_ID, null);
				if (po != null)
					break;
			}
		}

		
		if (po == null)
		{
			return "PO is Null";
		}
		
		int columnIndex = po.get_ColumnIndex(columnName);
		if(columnIndex < 0)
		{
			return "Error of column setting";
		}
		
		String[] keyColumns = po.get_KeyColumns();
		if(keyColumns.length > 1)
		{
			return "key column must be only one." ;
		}
		keyColumnName = keyColumns[0];
		
		if(po.getAD_Client_ID()==Env.getAD_Client_ID(Env.getCtx()))
		{
			isSameClientData = true;
		}
		
		return null;
	}

		
	private void zkInit() 
	{
		/*Main Layout(Borderlayout)*/
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		
		mainLayout.appendChild(north);

		//Search Parameter Panel
		north.appendChild(parameterPanel);
		north.setStyle("border: none");
		parameterPanel.appendChild(parameterLayout); 		//parameterLayout is Grid
		parameterLayout.setWidth("100%");
		Rows parameterLayoutRows = parameterLayout.newRows();
		Row row = null;

		
		row = parameterLayoutRows.newRow();   
		
		if(isMultiLingual)
		{
			String elementName = Msg.getElement(Env.getCtx(), MLanguage.COLUMNNAME_AD_Language);
			org.adempiere.webui.component.Label searchEditorLabel = new org.adempiere.webui.component.Label (elementName);
			row.appendCellChild(searchEditorLabel.rightAlign(),1);	
			
			int AD_Column_ID = MColumn.getColumn_ID(MClient.Table_Name, MClient.COLUMNNAME_AD_Language);
			MLookup lookup = MLookupFactory.get (Env.getCtx(), wysiwygEditorForm.getWindowNo(), 0, AD_Column_ID, DisplayType.Table);
	
			tableDirEditor = new WTableDirEditor("AD_Language", false, false, true, lookup);
			tableDirEditor.addValueChangeListener(this);
			row.appendCellChild(tableDirEditor.getComponent(),2);		
			
			//Popup Menu
			WEditorPopupMenu  popupMenu = tableDirEditor.getPopupMenu();
			List<Component> listcomp = popupMenu.getChildren();
			Menuitem menuItem = null;
			String image = null;
			for(Component comp : listcomp)
			{
				if(comp instanceof Menuitem)
				{
					menuItem = (Menuitem)comp;
					image = menuItem.getImage();
					if(image.endsWith("Zoom16.png")||image.endsWith("Refresh16.png")
							|| image.endsWith("New16.png") || image.endsWith("InfoBPartner16.png"))
					{
						menuItem.setVisible(true);
					}else{
						menuItem.setVisible(false);
					}
				}
			}//for
	
	        if (popupMenu != null)
	        {
	        	popupMenu.addMenuListener((ContextMenuListener)tableDirEditor);
	        	row.appendChild(popupMenu);
	        	popupMenu.addContextElement((XulElement) tableDirEditor.getComponent());
	        }
	        
		}//if(isMultiLingual)
				
		
    	//Button
		if(isSameClientData)
		{
			SaveButton = new Button(Msg.getMsg(Env.getCtx(), "save"));
			SaveButton.setId("SaveButton");
			SaveButton.addActionListener(this);
			SaveButton.setEnabled(true);
			SaveButton.setImage(ThemeManager.getThemeResource("images/Save16.png"));
			row.appendCellChild(SaveButton);
		}


		//for space under Button
		row = parameterLayoutRows.newRow();
				row.appendCellChild(new Space(),1);

		String imageDir = MSysConfig.getValue("JPIERE_CKEDITOR_IMAGE_DIR", Env.getAD_Client_ID(Env.getCtx()));
		if(imageDir==null)
		{
			imageDir = "images/";
		}
				
		//Edit Area
		mainLayout.appendChild(center);
			ckeditor.setFilebrowserImageUploadUrl(imageDir);
			ckeditor.setFilebrowserBrowseUrl(imageDir);
			ckeditor.setFilebrowserImageBrowseUrl(imageDir);
			ckeditor.setValue((String)po.get_Value(columnName));
			center.appendChild(ckeditor);
			
		mainLayout.appendChild(south);
		south.setHeight("0px");	
	}

	@Override
	public void onEvent(Event e) throws Exception 
	{

		if (e == null)
		{
			return;

		}else if(e.getTarget().equals(SaveButton)){

			if(po != null && !isMultiLingual)
			{
				po.set_ValueNoCheck(columnName, ckeditor.getValue());
				po.saveEx();
			}else if(isMultiLingual){
				
				if (tableDirEditor.getValue()== null || tableDirEditor.getValue().toString().equals(baselang)) 
				{
					po.set_ValueNoCheck(columnName, ckeditor.getValue());
					
					StringBuilder sqlupdate = new StringBuilder("UPDATE ")
						.append(baseTableName).append(" SET ").append(columnName).append("='").append(ckeditor.getValue()).append("'")
						.append(" WHERE ").append(keyColumnName).append("=").append(po.get_ID());
					int no = DB.executeUpdate(sqlupdate.toString(), null);
					if(no != 1)
					{
						throw new Exception(Msg.getMsg(Env.getCtx(), "SaveError"));//TODO:Do appropriate message:
					}
				}else{//Update Trl
					StringBuilder sqlupdate = new StringBuilder("UPDATE ")
						.append(trlTableName).append(" SET ").append(columnName).append("='").append(ckeditor.getValue()).append("'")
						.append(" WHERE ").append(keyColumnName).append("=").append(po.get_ID())
						.append(" AND AD_Language =").append(DB.TO_STRING(tableDirEditor.getValue().toString()));
					int no = DB.executeUpdate(sqlupdate.toString(), null);
					if(no != 1)
					{
						throw new Exception(Msg.getMsg(Env.getCtx(), "SaveError"));//TODO:Do appropriate message:
					}
				}
				
			}else{
				throw new Exception(Msg.getMsg(Env.getCtx(), "SaveError"));
			}

		}

	}//onEvent()

	
	
	public void valueChange(ValueChangeEvent e)
	{
		tableDirEditor.setValue(e.getNewValue());
		if (tableDirEditor.getValue()== null || tableDirEditor.getValue().toString().equals(baselang))
		{
			ckeditor.setValue((String)po.get_Value(columnName));	
			
		}else{
			StringBuilder sql = new StringBuilder("SELECT ").append(columnName).append(" FROM ").append(trlTableName)
										.append(" WHERE ").append(keyColumnName).append("=").append(po.get_ID())
										.append(" AND AD_Language =").append(DB.TO_STRING(tableDirEditor.getValue().toString()));
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement(sql.toString(), null);
				rs = pstmt.executeQuery();
				if(rs.next())
					ckeditor.setValue(rs.getString(1));
			}
			catch (Exception exception)
			{
				log.log(Level.SEVERE, sql.toString(), exception);
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null;
				pstmt = null;
			}
		}
		
	}//valueChange(ValueChangeEvent e)

}
