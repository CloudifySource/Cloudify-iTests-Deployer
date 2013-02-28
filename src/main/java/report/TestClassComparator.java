package deployer.report;

import org.testng.IClass;

import java.util.Comparator;

/**
* Comparator for sorting classes alphabetically by fully-qualified name.
* @author moran
*/
class TestClassComparator implements Comparator<IClass>
{
    public int compare(IClass class1, IClass class2)
    {
        return class1.getName().compareTo(class2.getName());
    }
}
