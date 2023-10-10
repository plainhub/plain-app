import{_ as x}from"./MonacoEditor.vuevuetypescriptsetuptruelang-5fc318be.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-7f3ac8e8.js";import{_ as b}from"./Breadcrumb-2d824363.js";import{d as N,u as $,r as s,i as C,k as r,a7 as p,bs as d,a1 as h,e as B,f as F,x as i,h as S,L as c,bt as g,o as U}from"./index-6d72ad8d.js";const q={class:"page-container"},D={class:"main"},Q=N({__name:"NetworkView",setup(A){const{t:u}=$(),o=s(0),n=s(""),t=s("");C({handle:(l,e)=>{e?r(u(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${d}
  `});const{mutate:_,loading:f,onDone:v}=h({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${d}
  `});v(()=>{r(u("saved"))});function V(){!n.value||!t.value||_({netplan:n.value,netmix:t.value})}return(l,e)=>{const k=b,w=y,m=x;return U(),B("div",q,[F("div",D,[i(k,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(w,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:V,loading:S(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),c(i(m,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[g,o.value===0]]),c(i(m,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{Q as default};
