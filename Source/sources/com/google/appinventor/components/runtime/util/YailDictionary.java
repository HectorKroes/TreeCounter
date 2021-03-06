package com.google.appinventor.components.runtime.util;

import android.support.annotation.NonNull;
import android.util.Log;
import com.google.appinventor.components.runtime.collect.Lists;
import com.google.appinventor.components.runtime.errors.DispatchableError;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import gnu.lists.FString;
import gnu.lists.LList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONException;

public class YailDictionary extends LinkedHashMap<Object, Object> implements YailObject<YailList> {
    public static final Object ALL = new Object() {
        public String toString() {
            return "ALL_ITEMS";
        }
    };
    private static final String LOG_TAG = "YailDictionary";

    private static class DictIterator implements Iterator<YailList> {

        /* renamed from: it */
        final Iterator<Entry<Object, Object>> f45it;

        DictIterator(Iterator<Entry<Object, Object>> it) {
            this.f45it = it;
        }

        public boolean hasNext() {
            return this.f45it.hasNext();
        }

        public YailList next() {
            Entry<Object, Object> e = (Entry) this.f45it.next();
            return YailList.makeList(new Object[]{e.getKey(), e.getValue()});
        }

        public void remove() {
            this.f45it.remove();
        }
    }

    public YailDictionary() {
    }

    public YailDictionary(Map<Object, Object> prevMap) {
        super(prevMap);
    }

    public static YailDictionary makeDictionary() {
        return new YailDictionary();
    }

    public static YailDictionary makeDictionary(Map<Object, Object> prevMap) {
        return new YailDictionary(prevMap);
    }

    public static YailDictionary makeDictionary(Object... keysAndValues) {
        if (keysAndValues.length % 2 == 1) {
            throw new IllegalArgumentException("Expected an even number of key-value entries.");
        }
        YailDictionary dict = new YailDictionary();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            dict.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return dict;
    }

    public static YailDictionary makeDictionary(List<YailList> pairs) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (YailList currentYailList : pairs) {
            Object currentKey = currentYailList.getObject(0);
            Object currentValue = currentYailList.getObject(1);
            if (!(currentValue instanceof YailList)) {
                map.put(currentKey, currentValue);
            } else if (isAlist((YailList) currentValue).booleanValue()) {
                map.put(currentKey, alistToDict((YailList) currentValue));
            } else {
                map.put(currentKey, checkList((YailList) currentValue));
            }
        }
        return new YailDictionary(map);
    }

    private static Boolean isAlist(YailList yailList) {
        boolean hadPair = false;
        Iterator it = ((LList) yailList.getCdr()).iterator();
        while (it.hasNext()) {
            Object currentPair = it.next();
            if (!(currentPair instanceof YailList)) {
                return Boolean.valueOf(false);
            }
            if (((YailList) currentPair).size() != 2) {
                return Boolean.valueOf(false);
            }
            hadPair = true;
        }
        return Boolean.valueOf(hadPair);
    }

    public static YailDictionary alistToDict(YailList alist) {
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
        Iterator it = ((LList) alist.getCdr()).iterator();
        while (it.hasNext()) {
            YailList currentPair = (YailList) it.next();
            Object currentKey = currentPair.getObject(0);
            Object currentValue = currentPair.getObject(1);
            if ((currentValue instanceof YailList) && isAlist((YailList) currentValue).booleanValue()) {
                map.put(currentKey, alistToDict((YailList) currentValue));
            } else if (currentValue instanceof YailList) {
                map.put(currentKey, checkList((YailList) currentValue));
            } else {
                map.put(currentKey, currentValue);
            }
        }
        return new YailDictionary(map);
    }

    private static YailList checkList(YailList list) {
        Object[] checked = new Object[list.size()];
        int i = 0;
        Iterator<?> it = list.iterator();
        it.next();
        boolean processed = false;
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof YailList)) {
                checked[i] = o;
            } else if (isAlist((YailList) o).booleanValue()) {
                checked[i] = alistToDict((YailList) o);
                processed = true;
            } else {
                checked[i] = checkList((YailList) o);
                if (checked[i] != o) {
                    processed = true;
                }
            }
            i++;
        }
        if (processed) {
            return YailList.makeList(checked);
        }
        return list;
    }

    private static YailList checkListForDicts(YailList list) {
        List<Object> copy = new ArrayList<>();
        Iterator it = ((LList) list.getCdr()).iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof YailDictionary) {
                copy.add(dictToAlist((YailDictionary) o));
            } else if (o instanceof YailList) {
                copy.add(checkListForDicts((YailList) o));
            } else {
                copy.add(o);
            }
        }
        return YailList.makeList((List) copy);
    }

    public static YailList dictToAlist(YailDictionary dict) {
        List<Object> list = new ArrayList<>();
        for (Entry<Object, Object> entry : dict.entrySet()) {
            list.add(YailList.makeList(new Object[]{entry.getKey(), entry.getValue()}));
        }
        return YailList.makeList((List) list);
    }

    public void setPair(YailList pair) {
        put(pair.getObject(0), pair.getObject(1));
    }

    private Object getFromList(List<?> target, Object currentKey) {
        int offset = target instanceof YailList ? 0 : 1;
        try {
            if (currentKey instanceof FString) {
                return target.get(Integer.parseInt(currentKey.toString()) - offset);
            }
            if (currentKey instanceof String) {
                return target.get(Integer.parseInt((String) currentKey) - offset);
            }
            if (currentKey instanceof Number) {
                return target.get(((Number) currentKey).intValue() - offset);
            }
            return null;
        } catch (NumberFormatException e) {
            Log.w(LOG_TAG, "Unable to parse key as integer: " + currentKey, e);
            throw new YailRuntimeError("Unable to parse key as integer: " + currentKey, "NumberParseException");
        } catch (IndexOutOfBoundsException e2) {
            Log.w(LOG_TAG, "Requested too large of an index: " + currentKey, e2);
            throw new YailRuntimeError("Requested too large of an index: " + currentKey, "IndexOutOfBoundsException");
        }
    }

    /* JADX WARNING: type inference failed for: r1v1, types: [java.lang.Object] */
    /* JADX WARNING: type inference failed for: r1v2 */
    /* JADX WARNING: type inference failed for: r1v5, types: [java.lang.Object] */
    /* JADX WARNING: type inference failed for: r2v4 */
    /* JADX WARNING: type inference failed for: r1v7, types: [java.lang.Object] */
    /* JADX WARNING: type inference failed for: r1v9, types: [java.lang.Object] */
    /* JADX WARNING: type inference failed for: r1v10 */
    /* JADX WARNING: type inference failed for: r1v11 */
    /* JADX WARNING: type inference failed for: r1v12 */
    /* JADX WARNING: type inference failed for: r1v13 */
    /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r1v2
      assigns: []
      uses: []
      mth insns count: 29
    	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
    	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
    	at jadx.core.ProcessClass.process(ProcessClass.java:30)
    	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:49)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:49)
    	at jadx.core.ProcessClass.process(ProcessClass.java:35)
    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
     */
    /* JADX WARNING: Unknown variable types count: 5 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Object getObjectAtKeyPath(java.util.List<?> r5) {
        /*
            r4 = this;
            r1 = r4
            java.util.Iterator r3 = r5.iterator()
        L_0x0005:
            boolean r2 = r3.hasNext()
            if (r2 == 0) goto L_0x0042
            java.lang.Object r0 = r3.next()
            boolean r2 = r1 instanceof java.util.Map
            if (r2 == 0) goto L_0x001a
            java.util.Map r1 = (java.util.Map) r1
            java.lang.Object r1 = r1.get(r0)
            goto L_0x0005
        L_0x001a:
            boolean r2 = r1 instanceof com.google.appinventor.components.runtime.util.YailList
            if (r2 == 0) goto L_0x0036
            r2 = r1
            com.google.appinventor.components.runtime.util.YailList r2 = (com.google.appinventor.components.runtime.util.YailList) r2
            java.lang.Boolean r2 = isAlist(r2)
            boolean r2 = r2.booleanValue()
            if (r2 == 0) goto L_0x0036
            com.google.appinventor.components.runtime.util.YailList r1 = (com.google.appinventor.components.runtime.util.YailList) r1
            com.google.appinventor.components.runtime.util.YailDictionary r2 = alistToDict(r1)
            java.lang.Object r1 = r2.get(r0)
            goto L_0x0005
        L_0x0036:
            boolean r2 = r1 instanceof java.util.List
            if (r2 == 0) goto L_0x0041
            java.util.List r1 = (java.util.List) r1
            java.lang.Object r1 = r4.getFromList(r1, r0)
            goto L_0x0005
        L_0x0041:
            r1 = 0
        L_0x0042:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.appinventor.components.runtime.util.YailDictionary.getObjectAtKeyPath(java.util.List):java.lang.Object");
    }

    private static Collection<Object> allOf(Map<Object, Object> map) {
        return map.values();
    }

    private static Collection<Object> allOf(List<Object> list) {
        if (!(list instanceof YailList)) {
            return list;
        }
        if (!isAlist((YailList) list).booleanValue()) {
            return (Collection) ((YailList) list).getCdr();
        }
        ArrayList<Object> result = new ArrayList<>();
        Iterator it = ((LList) ((YailList) list).getCdr()).iterator();
        while (it.hasNext()) {
            result.add(((YailList) it.next()).getObject(1));
        }
        return result;
    }

    private static Collection<Object> allOf(Object object) {
        if (object instanceof Map) {
            return allOf((Map) object);
        }
        if (object instanceof List) {
            return allOf((List) object);
        }
        return Collections.emptyList();
    }

    private static Object alistLookup(YailList alist, Object target) {
        Iterator it = ((LList) alist.getCdr()).iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof YailList)) {
                return null;
            }
            if (((YailList) o).getObject(0).equals(target)) {
                return ((YailList) o).getObject(1);
            }
        }
        return null;
    }

    private static <T> List<Object> walkKeyPath(Object root, List<T> keysOrIndices, List<Object> result) {
        if (keysOrIndices.isEmpty()) {
            if (root != null) {
                result.add(root);
            }
        } else if (root != null) {
            Object currentKey = keysOrIndices.get(0);
            List<T> childKeys = keysOrIndices.subList(1, keysOrIndices.size());
            if (currentKey == ALL) {
                for (Object child : allOf(root)) {
                    walkKeyPath(child, childKeys, result);
                }
            } else if (root instanceof Map) {
                walkKeyPath(((Map) root).get(currentKey), childKeys, result);
            } else if ((root instanceof YailList) && isAlist((YailList) root).booleanValue()) {
                Object value = alistLookup((YailList) root, currentKey);
                if (value != null) {
                    walkKeyPath(value, childKeys, result);
                }
            } else if (root instanceof List) {
                try {
                    walkKeyPath(((List) root).get(keyToIndex((List) root, currentKey)), childKeys, result);
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    public static <T> List<Object> walkKeyPath(YailObject<?> object, List<T> keysOrIndices) {
        return walkKeyPath(object, keysOrIndices, new ArrayList());
    }

    private static int keyToIndex(List<?> target, Object key) {
        int offset;
        int index;
        if (target instanceof YailList) {
            offset = 0;
        } else {
            offset = 1;
        }
        if (key instanceof Number) {
            index = ((Number) key).intValue();
        } else {
            try {
                index = Integer.parseInt(key.toString());
            } catch (NumberFormatException e) {
                throw new DispatchableError(ErrorMessages.ERROR_NUMBER_FORMAT_EXCEPTION, key.toString());
            }
        }
        int index2 = index - offset;
        if (index2 >= 0 && index2 < (target.size() + 1) - offset) {
            return index2;
        }
        try {
            throw new DispatchableError(ErrorMessages.ERROR_INDEX_MISSING_IN_LIST, Integer.valueOf(index2 + offset), JsonUtil.getJsonRepresentation(target));
        } catch (JSONException e2) {
            Log.e(LOG_TAG, "Unable to serialize object as JSON", e2);
            throw new YailRuntimeError(e2.getMessage(), "JSON Error");
        }
    }

    private Object lookupTargetForKey(Object target, Object key) {
        String simpleName;
        if (target instanceof YailDictionary) {
            return ((YailDictionary) target).get(key);
        }
        if (target instanceof List) {
            return ((List) target).get(keyToIndex((List) target, key));
        }
        Object[] objArr = new Object[1];
        if (target == null) {
            simpleName = "null";
        } else {
            simpleName = target.getClass().getSimpleName();
        }
        objArr[0] = simpleName;
        throw new DispatchableError(ErrorMessages.ERROR_INVALID_VALUE_IN_PATH, objArr);
    }

    /* JADX WARNING: type inference failed for: r5v4 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setValueForKeyPath(java.util.List<?> r8, java.lang.Object r9) {
        /*
            r7 = this;
            r3 = r7
            java.util.Iterator r0 = r8.iterator()
            boolean r4 = r8.isEmpty()
            if (r4 == 0) goto L_0x005d
        L_0x000b:
            return
        L_0x000c:
            boolean r4 = r0.hasNext()
            if (r4 == 0) goto L_0x000b
            java.lang.Object r1 = r0.next()
            boolean r4 = r0.hasNext()
            if (r4 == 0) goto L_0x0022
            java.lang.Object r3 = r7.lookupTargetForKey(r5, r1)
            r5 = r3
            goto L_0x000c
        L_0x0022:
            boolean r4 = r5 instanceof com.google.appinventor.components.runtime.util.YailDictionary
            if (r4 == 0) goto L_0x002d
            r4 = r5
            com.google.appinventor.components.runtime.util.YailDictionary r4 = (com.google.appinventor.components.runtime.util.YailDictionary) r4
            r4.put(r1, r9)
            goto L_0x000c
        L_0x002d:
            boolean r4 = r5 instanceof com.google.appinventor.components.runtime.util.YailList
            if (r4 == 0) goto L_0x0043
            r2 = r5
            gnu.lists.LList r2 = (gnu.lists.LList) r2
            r4 = r5
            java.util.List r4 = (java.util.List) r4
            int r4 = keyToIndex(r4, r1)
            gnu.lists.SeqPosition r4 = r2.getIterator(r4)
            r4.set(r9)
            goto L_0x000c
        L_0x0043:
            boolean r4 = r5 instanceof java.util.List
            if (r4 == 0) goto L_0x0055
            r4 = r5
            java.util.List r4 = (java.util.List) r4
            r6 = r5
            java.util.List r6 = (java.util.List) r6
            int r6 = keyToIndex(r6, r1)
            r4.set(r6, r9)
            goto L_0x000c
        L_0x0055:
            com.google.appinventor.components.runtime.errors.DispatchableError r4 = new com.google.appinventor.components.runtime.errors.DispatchableError
            r5 = 3203(0xc83, float:4.488E-42)
            r4.<init>(r5)
            throw r4
        L_0x005d:
            r5 = r3
            goto L_0x000c
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.appinventor.components.runtime.util.YailDictionary.setValueForKeyPath(java.util.List, java.lang.Object):void");
    }

    public boolean containsKey(Object key) {
        if (key instanceof FString) {
            return super.containsKey(key.toString());
        }
        return super.containsKey(key);
    }

    public boolean containsValue(Object value) {
        if (value instanceof FString) {
            return super.containsValue(value.toString());
        }
        return super.containsValue(value);
    }

    public Object get(Object key) {
        if (key instanceof FString) {
            return super.get(key.toString());
        }
        return super.get(key);
    }

    public Object put(Object key, Object value) {
        if (key instanceof FString) {
            key = key.toString();
        }
        if (value instanceof FString) {
            value = value.toString();
        }
        return super.put(key, value);
    }

    public Object remove(Object key) {
        if (key instanceof FString) {
            return super.remove(key.toString());
        }
        return super.remove(key);
    }

    public String toString() {
        try {
            return JsonUtil.getJsonRepresentation(this);
        } catch (JSONException e) {
            throw new YailRuntimeError(e.getMessage(), "JSON Error");
        }
    }

    public Object getObject(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException();
        }
        int i = index;
        for (Entry<Object, Object> e : entrySet()) {
            if (i == 0) {
                return Lists.newArrayList(e.getKey(), e.getValue());
            }
            i--;
        }
        throw new IndexOutOfBoundsException();
    }

    @NonNull
    public Iterator<YailList> iterator() {
        return new DictIterator(entrySet().iterator());
    }
}
