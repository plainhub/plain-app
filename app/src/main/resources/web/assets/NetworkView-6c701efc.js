import{_ as k}from"./MonacoEditor.vuevuetypescriptsetuptruelang-5347af17.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-3983f8cd.js";import{_ as b}from"./Breadcrumb-d0e8b352.js";import{d as N,u as $,r as m,g as C,x as r,ab as p,bD as c,i as h,c as B,a as D,p as i,j as F,m as d,aW as g,o as S}from"./index-e895cc69.js";const U={class:"page-container"},q={class:"main"},Q=N({__name:"NetworkView",setup(j){const{t:s}=$(),o=m(0),n=m(""),a=m("");C({handle:(l,e)=>{e?r(s(e),"error"):(n.value=l.networkConfig.netplan,a.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});const{mutate:_,loading:f,onDone:v}=h({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});v(()=>{r(s("saved"))});function V(){!n.value||!a.value||_({netplan:n.value,netmix:a.value})}return(l,e)=>{const w=b,x=y,u=k;return S(),B("div",U,[D("div",q,[i(w,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(x,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=t=>o.value=t),save:V,loading:F(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(u,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=t=>n.value=t)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(u,{language:"yaml",height:"700",modelValue:a.value,"onUpdate:modelValue":e[2]||(e[2]=t=>a.value=t)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{Q as default};
