package stroom.config.global.client.presenter;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.cell.expander.client.ExpanderCell;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.entity.client.presenter.TreeRowHandler;
import stroom.util.shared.Expander;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfigPropertyClusterValuesListPresenter
        extends MyPresenterWidget<DataGridView<ClusterValuesRow>> {

    private Column<ClusterValuesRow, Expander> expanderColumn;
    private ListDataProvider<ClusterValuesRow> dataProvider;
    private ClusterValuesTreeAction treeAction = new ClusterValuesTreeAction();

    private Map<String, Set<NodeSource>> effectiveValueToNodeSourcesMap;

    @Inject
    public ConfigPropertyClusterValuesListPresenter(final EventBus eventBus) {
        super(eventBus, new DataGridViewImpl<>(false, 1000));
        this.dataProvider = new ListDataProvider<>();
        initTableColumns();

        dataProvider.addDataDisplay(getView().getDataDisplay());

//         Handle use of the expander column.
        dataProvider.setTreeRowHandler(new TreeRowHandler<>(treeAction, getView(), expanderColumn));
    }

    // For DEV testing only, when you don't have two nodes
    private Map<String, Set<String>> makeDemoData() {
        Supplier<String> junkTextSupplier = () ->
            IntStream.rangeClosed(1,5)
                .boxed()
                .map(i -> this.getClass().getCanonicalName())
                .collect(Collectors.joining(" "));

        Map<String, Set<String>>  demoMap = new HashMap<>();
        IntStream.rangeClosed(1, 9)
            .forEach(i -> {
                demoMap.put("value " + i + junkTextSupplier.get(), IntStream.rangeClosed(i*10,(i*10)+9)
                    .boxed()
                    .map(j -> "node" + j)
                    .collect(Collectors.toSet()));
            });
        return demoMap;
    }

    public void setData(final Map<String, Set<NodeSource>> effectiveValueToNodesMap) {

        // For DEV testing only, when you don't have two nodes
//        this.effectiveValueToNodesMap = makeDemoData();
        this.effectiveValueToNodeSourcesMap = effectiveValueToNodesMap;

        refresh();
    }

    private void initTableColumns() {
        // Expander column.
        expanderColumn = new Column<ClusterValuesRow, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final ClusterValuesRow row) {
                return buildExpander(row);
            }
            @Override
            public String getCellStyleNames(Cell.Context context, ClusterValuesRow object) {
                return super.getCellStyleNames(context, object) + " "
                    + getView().getResources().dataGridStyle().dataGridCellVerticalTop();
            }
        };
        expanderColumn.setFieldUpdater((index, row, value) -> {
            treeAction.setRowExpanded(row, !value.isExpanded());
            refresh();
        });

        getView().addColumn(expanderColumn, "");
        getView().addResizableColumn(buildEffectiveValueColumn(), "Effective Value", 475);
        getView().addResizableColumn(buildNodeCountColumn(), "Count", 50);
        getView().addResizableColumn(buildBasicColumn(ClusterValuesRow::getSource), "Source", 75);
        getView().addResizableColumn(buildBasicColumn(ClusterValuesRow::getNodeName), "Node", 250);
        getView().addEndColumn(new EndColumn<>());
    }

    private Column<ClusterValuesRow, String> buildNodeCountColumn() {
        return buildBasicColumn(row ->
            (row.getNodeCount() != null ? row.getNodeCount().toString() : ""));
    }

    private Column<ClusterValuesRow, String> buildBasicColumn(final Function<ClusterValuesRow, String> valueFunc) {
        final Column<ClusterValuesRow, String> column = new Column<ClusterValuesRow, String>(new TextCell()) {
            @Override
            public String getValue(final ClusterValuesRow row) {
                if (row == null) {
                    return null;
                }
                return valueFunc.apply(row);
            }
        };
        return column;
    }

    private Column<ClusterValuesRow, String> buildEffectiveValueColumn() {
        final Column<ClusterValuesRow, String> column = new Column<ClusterValuesRow, String>(new TextCell()) {
            @Override
            public String getValue(final ClusterValuesRow row) {
                if (row == null) {
                    return null;
                }
                return row.getEffectiveValue();
            }

            @Override
            public String getCellStyleNames(Cell.Context context, ClusterValuesRow object) {
                return super.getCellStyleNames(context, object) + " "
                    + getView().getResources().dataGridStyle().dataGridCellWrapText();
            }
        };
        return column;
    }

    private Expander buildExpander(final ClusterValuesRow row) {
        return row.getExpander();
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        refresh();
    }

    public void refresh() {
        final List<ClusterValuesRow> rows = ClusterValuesRow.buildTree(effectiveValueToNodeSourcesMap, treeAction);
        dataProvider.setCompleteList(rows);
        dataProvider.refresh(true);
    }
}
