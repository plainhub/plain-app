import{_ as y}from"./MonacoEditor.vuevuetypescriptsetuptruelang-a762fc4a.js";import{_ as k}from"./EditToolbar.vuevuetypescriptsetuptruelang-b0512ce8.js";import{_ as b}from"./Breadcrumb-120bdd32.js";import{d as N,u as $,r as s,i as C,t as r,a7 as p,bx as c,a3 as h,o as B,c as F,e as S,x as i,j as U,O as d,by as g}from"./index-df35a132.js";const q={class:"page-container"},D={class:"main"},O=N({__name:"NetworkView",setup(j){const{t:u}=$(),o=s(0),n=s(""),t=s("");C({handle:(l,e)=>{e?r(u(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
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
  `});v(()=>{r(u("saved"))});function x(){!n.value||!t.value||_({netplan:n.value,netmix:t.value})}return(l,e)=>{const V=b,w=k,m=y;return B(),F("div",q,[S("div",D,[i(V,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(w,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:x,loading:U(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(m,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(m,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{O as default};
