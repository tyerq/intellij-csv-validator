package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CsvBlockElement extends CsvBlock {

    public CsvBlockElement(ASTNode node, CsvFormattingInfo formattingInfo) {
        this(node, formattingInfo, null);
    }
    
    public CsvBlockElement(ASTNode node, CsvFormattingInfo formattingInfo, CsvBlockField field) {
        super(node, formattingInfo);
        setField(field);
    }
    
    public CsvColumnInfo getColumnInfo() {
        return getField() == null ? null : getField().getColumnInfo();
    }
    
    private CsvBlockField field;
    
    public CsvBlockField getField() {
        return field;
    }
    
    public void setField(CsvBlockField field) {
        this.field = field;
    }

    @Override
    protected List<Block> buildChildren() {
        return Collections.emptyList();
    }

    @Override
    public Indent getIndent() {
        if (formattingInfo.getCsvCodeStyleSettings().TABULARIZE
                && (formattingInfo.getCsvCodeStyleSettings().LEADING_WHITE_SPACES || this.getElementType() == CsvTypes.COMMA)
                && (formattingInfo.getCsvCodeStyleSettings().WHITE_SPACES_OUTSIDE_QUOTES || !CsvFormatHelper.isQuotedField(getField()))
                && getField() != null) {
            CsvColumnInfo columnInfo = formattingInfo.getColumnInfo(0);
            return Indent.getSpaceIndent(columnInfo.getMaxLength() - getField().getTextLength() + getAdditionalSpaces(null, this));
        } else if (!formattingInfo.getCsvCodeStyleSettings().TABULARIZE &&
                this.getElementType() == CsvTypes.COMMA &&
                formattingInfo.getCsvCodeStyleSettings().SPACE_BEFORE_SEPARATOR) {
            return Indent.getSpaceIndent(1);
        }
        return null;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block block, @NotNull Block block1) {
        return null;
    }
}
