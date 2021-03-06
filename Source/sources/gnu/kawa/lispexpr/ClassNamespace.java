package gnu.kawa.lispexpr;

import gnu.bytecode.ClassType;
import gnu.kawa.functions.GetNamedPart;
import gnu.mapping.Namespace;
import gnu.mapping.WrappedException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

public class ClassNamespace extends Namespace implements Externalizable {
    ClassType ctype;

    public ClassType getClassType() {
        return this.ctype;
    }

    public static ClassNamespace getInstance(String name, ClassType ctype2) {
        synchronized (nsTable) {
            Object old = nsTable.get(name);
            if (old instanceof ClassNamespace) {
                ClassNamespace classNamespace = (ClassNamespace) old;
                return classNamespace;
            }
            ClassNamespace ns = new ClassNamespace(ctype2);
            nsTable.put(name, ns);
            return ns;
        }
    }

    public ClassNamespace() {
    }

    public ClassNamespace(ClassType ctype2) {
        setName("class:" + ctype2.getName());
        this.ctype = ctype2;
    }

    public Object get(String name) {
        try {
            return GetNamedPart.getTypePart(this.ctype, name);
        } catch (Throwable ex) {
            throw WrappedException.wrapIfNeeded(ex);
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.ctype);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.ctype = (ClassType) in.readObject();
        setName("class:" + this.ctype.getName());
    }

    public Object readResolve() throws ObjectStreamException {
        String name = getName();
        if (name != null) {
            Namespace ns = (Namespace) nsTable.get(name);
            if (ns instanceof ClassNamespace) {
                return ns;
            }
            nsTable.put(name, this);
        }
        return this;
    }
}
