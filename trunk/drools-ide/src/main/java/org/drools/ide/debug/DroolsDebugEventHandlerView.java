package org.drools.ide.debug;

import org.drools.ide.DroolsIDEPlugin;
import org.drools.ide.debug.actions.ShowLogicalStructureAction;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.VariablesViewModelPresentation;
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.internal.ui.contexts.provisional.IDebugContextListener;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A generic Drools debug view.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">kris verlaenen </a>
 */
public abstract class DroolsDebugEventHandlerView extends AbstractDebugView implements IDebugContextListener, ISelectionListener {

    private VariablesViewModelPresentation modelPresentation;
    private boolean showLogical = true;

    public void dispose() {
		DebugContextManager.getDefault().removeDebugContextListener(this, getSite().getWorkbenchWindow());
        getSite().getPage().removeSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, this);
        super.dispose();
    }

    public boolean isShowLogicalStructure() {
        return showLogical;
    }

    public void setShowLogicalStructure(boolean showLogical) {
        this.showLogical = showLogical;
    }

    protected void setViewerInput(Object context) {
    	Object input = null;
    	if (context instanceof IJavaStackFrame) {
            try {
                IJavaObject stackObj = ((IJavaStackFrame) context).getThis();
                if ((stackObj != null)
                        && (stackObj.getJavaType() != null)
                        && ("org.drools.reteoo.ReteooWorkingMemory".equals(
                            stackObj.getJavaType().getName()))) {
                    input = stackObj;
                }
            } catch (Throwable t) {
                DroolsIDEPlugin.log(t);
            }
        } else if (context instanceof IVariable) {
        	IVariable variable = (IVariable) context;
        	
        	// find out if stackFrame is inside a WorkingMemoryImpl
        	// if so, use that, no matter what variable is selected
        	ISelection stackSelection = DebugContextManager.getDefault().getActiveContext(getSite().getWorkbenchWindow());
            if (stackSelection instanceof IStructuredSelection) {
                Object stack = ((IStructuredSelection) stackSelection).getFirstElement();
                if (stack instanceof IJavaStackFrame) {
                    try {
                        IJavaObject stackObj = ((IJavaStackFrame) stack).getThis();
                        if ((stackObj != null)
                                && (stackObj.getJavaType() != null)
                                && ("org.drools.reteoo.ReteooWorkingMemory".equals(
                                    stackObj.getJavaType().getName()))) {
                            input = stackObj;
                        }
                    } catch (Throwable t) {
                        // do nothing
                    }
                }
            }
            if (input == null) {
                try {
                    IValue value = ((IVariable) context).getValue();
                    if (value != null && value instanceof IJavaObject
                            && "org.drools.reteoo.ReteooWorkingMemory".equals(
                                variable.getValue().getReferenceTypeName())) {
                        input = value;
                    }
                } catch (Throwable t) {
                    DroolsIDEPlugin.log(t);
                }
            }
        }
    	
    	Object current = getViewer().getInput();
				
		if (current == null && input == null) {
			return;
		}

		if (current != null && current.equals(input)) {
			return;
		}
		
		showViewer();
		getViewer().setInput(input);
    }

    public Viewer createViewer(Composite parent) {
		TreeViewer variablesViewer = new TreeViewer(parent);
		variablesViewer.setContentProvider(createContentProvider());
        variablesViewer.setLabelProvider(new VariablesViewLabelProvider(
            getModelPresentation()));
        variablesViewer.setUseHashlookup(true);
		DebugContextManager.getDefault().addDebugContextListener(this, getSite().getWorkbenchWindow());
        getSite().getPage().addSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, this);
		return variablesViewer;
    }
    
    protected abstract IContentProvider createContentProvider();

    protected String getHelpContextId() {
        return null;
    }

	protected void becomesHidden() {
		setViewerInput(null);
		super.becomesHidden();
	}

	protected void becomesVisible() {
		super.becomesVisible();
        ISelection selection = getSite().getPage().getSelection(
            IDebugUIConstants.ID_VARIABLE_VIEW);
        if (selection instanceof IStructuredSelection) {
            setViewerInput(((IStructuredSelection) selection).getFirstElement());
        }
    }

	protected void createActions() {
        IAction action = new ShowLogicalStructureAction(this);
        setAction("ShowLogicalStructure", action);
    }

    protected void configureToolBar(IToolBarManager tbm) {
        tbm.add(getAction("ShowLogicalStructure"));
    }

    protected void fillContextMenu(IMenuManager menu) {
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    }

	public void contextActivated(ISelection selection, IWorkbenchPart part) {
		if (!isAvailable() || !isVisible()) {
			return;
		}
		
		if (selection instanceof IStructuredSelection) {
			setViewerInput(((IStructuredSelection)selection).getFirstElement());
		}
		showViewer();
	}

	public void contextChanged(ISelection selection, IWorkbenchPart part) {
	}
	
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (!isAvailable()) {
            return;
        }
        if (selection == null) {
            setViewerInput(null);
        } else if (selection instanceof IStructuredSelection) {
            setViewerInput(((IStructuredSelection) selection).getFirstElement());
        }
    }
    
	protected void initActionState(IAction action) {
		// The show logical structure action is always enabled by default
		// when (re)starting the view 
		String id = action.getId();
		if (id.endsWith("ShowLogicalStructureAction")) {
			action.setChecked(true);
		} else {
			super.initActionState(action);
		}
	}
    
    protected IDebugModelPresentation getModelPresentation() {
        if (modelPresentation == null) {
            modelPresentation = new VariablesViewModelPresentation();
        }
        return modelPresentation;
    }

	private class VariablesViewLabelProvider implements ILabelProvider, IColorProvider {

        private IDebugModelPresentation presentation;

        public VariablesViewLabelProvider(IDebugModelPresentation presentation) {
            this.presentation = presentation;
        }

        public IDebugModelPresentation getPresentation() {
            return presentation;
        }

        public Image getImage(Object element) {
            return presentation.getImage(element);
        }

        public String getText(Object element) {
            return presentation.getText(element);
        }

        public void addListener(ILabelProviderListener listener) {
            presentation.addListener(listener);
        }

        public void dispose() {
            presentation.dispose();
        }

        public boolean isLabelProperty(Object element, String property) {
            return presentation.isLabelProperty(element, property);
        }

        public void removeListener(ILabelProviderListener listener) {
            presentation.removeListener(listener);
        }

        public Color getForeground(Object element) {
            if (element instanceof IVariable) {
                IVariable variable = (IVariable) element;
                try {
                    if (variable.hasValueChanged()) {
                        return JFaceResources.getColorRegistry()
                            .get(IDebugUIConstants.PREF_CHANGED_DEBUG_ELEMENT_COLOR);
                    }
                } catch (DebugException e) {
                    DroolsIDEPlugin.log(e);
                }
            }
            return null;
        }

        public Color getBackground(Object element) {
            return null;
        }

    }
}
