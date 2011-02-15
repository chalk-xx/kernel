package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

public interface SimpleContentImportListener {

  void onReorder(String orderedPath, String beforeSibbling);

  void onMove(String srcPath, String destPath);

  void onModify(String srcPath);

  void onDelete(String srcPath);

  void onCreate(String srcPath);

  void onCopy(String srcPath, String destPath);

}
