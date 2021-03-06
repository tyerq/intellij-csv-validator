package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CsvIntentionHelper {
    private static final Logger LOG = Logger.getInstance("#net.seesharpsoft.intellij.plugins.csv.inspection.CsvIntentionHelper");

    public static List<PsiElement> getChildren(PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        if (element != null) {
            element = element.getFirstChild();
            while(element != null) {
                children.add(element);
                element = element.getNextSibling();
            }
        }
        return children;
    }
    
    public static IElementType getElementType(PsiElement element) {
        return element == null || element.getNode() == null ? null : element.getNode().getElementType();
    }

    public static PsiElement getParentFieldElement(PsiElement element) {
        if (getElementType(element) == TokenType.WHITE_SPACE) {
            if (getElementType(element.getParent()) == CsvTypes.FIELD) {
                element = element.getParent();
            } else if (getElementType(element.getPrevSibling()) == CsvTypes.FIELD) {
                element = element.getPrevSibling();
            } else {
                element = null;
            }
        } else {
            while (element != null && CsvIntentionHelper.getElementType(element) != CsvTypes.FIELD) {
                element = element.getParent();
            }
        }
        return element;
    }
    
    private static PsiElement getPreviousSeparator(PsiElement fieldElement) {
        while (fieldElement != null) {
            if (getElementType(fieldElement) == CsvTypes.COMMA) {
                break;
            }
            fieldElement = fieldElement.getPrevSibling();
        }
        return fieldElement;
    }

    private static PsiElement getNextSeparator(PsiElement fieldElement) {
        while (fieldElement != null) {
            if (getElementType(fieldElement) == CsvTypes.COMMA) {
                break;
            }
            fieldElement = fieldElement.getNextSibling();
        }
        return fieldElement;
    }

    public static Collection<PsiElement> getAllElements(PsiFile file) {
        List<PsiElement> todo = getChildren(file);
        Collection<PsiElement> elements = new HashSet();
        while(todo.size() > 0) {
            PsiElement current = todo.get(todo.size() - 1);
            todo.remove(todo.size() - 1);
            elements.add(current);
            todo.addAll(getChildren(current));
        }
        return elements;
    }
    
    private static Collection<PsiElement> getAllFields(PsiFile file) {
        return getChildren(file).parallelStream()
                .filter(element -> getElementType(element) == CsvTypes.RECORD)
                .flatMap(record -> getChildren(record).stream())
                .filter(element -> getElementType(element) == CsvTypes.FIELD)
                .collect(Collectors.toList());
    }
    
    public static void quoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            List<Integer> quotePositions = new ArrayList<>();
            Collection<PsiElement> fields = getAllFields(psiFile);
            PsiElement separator;
            for (PsiElement field : fields) {
                if (field.getFirstChild() == null || getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                    separator = getPreviousSeparator(field);
                    if (separator == null) {
                        quotePositions.add(field.getParent().getTextOffset());
                    } else {
                        quotePositions.add(separator.getTextOffset() + separator.getTextLength());
                    }
                }
                if (field.getLastChild() == null || getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
                    separator = getNextSeparator(field);
                    if (separator == null) {
                        quotePositions.add(field.getParent().getTextOffset() + field.getParent().getTextLength());
                    } else {
                        quotePositions.add(separator.getTextOffset());
                    }
                }
            }
            String text = addQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void unquoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            List<Integer> quotePositions = new ArrayList<>();
            Collection<PsiElement> fields = getAllFields(psiFile);
            for (PsiElement field : fields) {
                if (getChildren(field).stream().anyMatch(element -> getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
                    continue;
                }
                if (getElementType(field.getFirstChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getFirstChild().getTextOffset());
                }
                if (getElementType(field.getLastChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getLastChild().getTextOffset());
                }
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }
    
    public static void quoteValue(@NotNull Project project, @NotNull PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

            element = getParentFieldElement(element);
            int quotePosition = getOpeningQuotePosition(element.getFirstChild(), element.getLastChild());
            if (quotePosition != -1) {
                quotePositions.add(quotePosition);
            }
            PsiElement endSeparatorElement = findQuotePositionsUntilSeparator(element, quotePositions);
            if (endSeparatorElement == null) {
                quotePositions.add(document.getTextLength());
            } else {
                quotePositions.add(endSeparatorElement.getTextOffset());
            }
            String text = addQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void unquoteValue(@NotNull Project project, @NotNull PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

            element = getParentFieldElement(element);
            if (getElementType(element.getFirstChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getFirstChild().getTextOffset());
            }
            if (getElementType(element.getLastChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getLastChild().getTextOffset());
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static String addQuotes(String text, List<Integer> quotePositions) {
        int offset = 0;
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            text = text.substring(0, offsetPosition) + "\"" + text.substring(offsetPosition);
            ++offset;
        }
        return text;
    }
    
    public static String removeQuotes(String text, List<Integer> quotePositions) {
        int offset = 0;
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            text = text.substring(0, offsetPosition) + text.substring(offsetPosition + 1);
            --offset;
        }
        return text;
    }

    public static int getOpeningQuotePosition(PsiElement firstFieldElement, PsiElement lastFieldElement) {
        if (getElementType(firstFieldElement) != CsvTypes.QUOTE) {
            return firstFieldElement.getTextOffset();
        }
        if (getElementType(lastFieldElement) == CsvTypes.QUOTE) {
            return lastFieldElement.getTextOffset();
        }
        return -1;
    }

    public static int getOpeningQuotePosition(PsiElement errorElement) {
        PsiElement lastFieldElement = errorElement;
        while(getElementType(lastFieldElement) != CsvTypes.RECORD) {
            lastFieldElement = lastFieldElement.getPrevSibling();
        }
        lastFieldElement = lastFieldElement.getLastChild();
        if (getElementType(lastFieldElement) != CsvTypes.FIELD) {
            throw new RuntimeException("Field element expected");
        }
        return getOpeningQuotePosition(lastFieldElement.getFirstChild(), lastFieldElement.getLastChild());

    }

    public static PsiElement findQuotePositionsUntilSeparator(PsiElement element, List<Integer> quotePositions) {
        PsiElement separatorElement = null;
        while (separatorElement == null && element != null) {
            if (getElementType(element) == CsvTypes.COMMA || getElementType(element) == CsvTypes.CRLF) {
                separatorElement = element;
                continue;
            }
            if (element.getFirstChild() != null) {
                separatorElement = findQuotePositionsUntilSeparator(element.getFirstChild(), quotePositions);
            } else if (element.getText().equals("\"")) {
                quotePositions.add(element.getTextOffset());
            }
            element = element.getNextSibling();
        }
        return separatorElement;
    }
    
}
