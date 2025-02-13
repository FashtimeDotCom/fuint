package com.fuint.base.util;

import com.fuint.base.service.entities.TreeNode;
import com.fuint.util.StringUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * 树形展示工具类
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
public class TreeUtil {

    /**
     * 创建菜单树状结构
     *
     * @param sourceTreeNodeList 分类node原数据
     * @return 树结构集合
     */
    public static List<TreeNode> sourceTreeNodes(List<TreeNode> sourceTreeNodeList) {
        //拼装树形结构
        List<TreeNode> nodeList = new ArrayList<TreeNode>();
        for (TreeNode node1 : sourceTreeNodeList) {
            boolean mark = false;
            for (TreeNode node2 : sourceTreeNodeList) {
                if (StringUtil.isNotEmpty(node1.getName()) && node1.getpId() == node2.getId()) {
                    mark = true;
                    if (node2.getChildrens() == null)
                        node2.setChildrens(new ArrayList<TreeNode>());
                    node2.getChildrens().add(node1);
                    break;
                }
            }
            if (!mark) {
                nodeList.add(node1);
            }
        }
        return nodeList;
    }

    public static void convert(List<TreeNode> sourceTreeNodes, List<TreeNode> treeNodes) {
        if (sourceTreeNodes != null && sourceTreeNodes.size() > 0) {
            TreeNode treeNode = null;
            StringBuffer nodeName = null;
            for (TreeNode sourceTreeNode : sourceTreeNodes) {
                treeNode = new TreeNode();
                nodeName = new StringBuffer();
                for(int i = 1 ; i < sourceTreeNode.getLevel() ; i ++){
                    nodeName.append("----");
                }
                nodeName.append(sourceTreeNode.getName());
                treeNode.setName(nodeName.toString());
                treeNode.setId(sourceTreeNode.getId());
                treeNodes.add(treeNode);
                if (sourceTreeNode.getChildrens() != null && sourceTreeNodes.size() > 0) {
                    convert(sourceTreeNode.getChildrens(), treeNodes);
                } else {
                    continue;
                }
            }
        }
    }
}
