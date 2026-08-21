import { Role } from "@/types/role";
import request from "@/utils/request";

export function getRoleList(){
    return request.get<Role[]>('/roles')
}