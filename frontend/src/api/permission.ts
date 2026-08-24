import { PermissionTreeNode } from "@/types/permission";
import request from "@/utils/request";


export function getPermissionTree(){
    return request.get<PermissionTreeNode[]>('/permissions')
}